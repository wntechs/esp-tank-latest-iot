package com.wntechs.tankcontroller.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.wntechs.tankcontroller.ui.viewmodel.MqttProvisioningPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class BleDeviceInfo(
    val device_id: String = "",
    val wifi_connected: Boolean = false,
    val ssid: String = "",
    val state: String = ""
)

@Serializable
data class WifiScanEvent(
    val state: String,
    val ssid: String? = null,
    val rssi: Int? = null,
    val secure: Boolean? = null
)

@Serializable
data class WifiScanResult(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean = true
)

@Serializable
data class BleStatusEvent(
    val state: String,
    val count: Int? = null,
    val message: String? = null
)

@Serializable
data class BleStatus(
    val status: String,
    val message: String? = null
)

@Serializable
private data class BleCommand(
    @SerialName("cmd") val cmd: String,
    @SerialName("ssid") val ssid: String? = null,
    @SerialName("password") val password: String? = null
)



data class BleScanItem(
    val device: BluetoothDevice,
    val address: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: List<UUID> = emptyList()
)


class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"

        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890A0")
        private val CHAR_DEVICE_INFO_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890A1")
        private val CHAR_COMMAND_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890A2")
        private val CHAR_STATUS_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890A3")
        private val CHAR_WIFI_SCAN_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890A4")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val pendingCommandWrites = ArrayDeque<String>()
    private var commandWriteInFlight = false
    private val wifiScanBuffer = StringBuilder()
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val seenDevices = linkedMapOf<String, BleScanItem>()
    private val pendingNotificationUuids = ArrayDeque<UUID>()

    private val _scanResults = MutableSharedFlow<BleScanItem>(extraBufferCapacity = 64)
    val scanResults = _scanResults.asSharedFlow()

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<BleDeviceInfo?>(null)
    val deviceInfo = _deviceInfo.asStateFlow()

    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _claimCode = MutableStateFlow("")
    val claimCode = _claimCode.asStateFlow()

    private val _wifiNetworks = MutableSharedFlow<WifiScanResult>(extraBufferCapacity = 64)
    val wifiNetworks = _wifiNetworks.asSharedFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private data class PendingWifiNetwork(
        val index: Int,
        val rssi: Int,
        val secure: Boolean,
        val ssidParts: MutableMap<Int, String> = sortedMapOf()
    )

    private val pendingWifiNetworks = mutableMapOf<Int, PendingWifiNetwork>()

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _status.value = "BLE scan failed: $errorCode"
            Log.e(TAG, "BLE scan failed with errorCode=$errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: return

        val item = BleScanItem(
            device = device,
            address = address,
            name = device.name ?: result.scanRecord?.deviceName,
            rssi = result.rssi,
            serviceUuids = result.scanRecord
                ?.serviceUuids
                ?.mapNotNull { it.uuid }
                .orEmpty()
        )

        val previous = seenDevices[address]
        val changed = previous == null ||
                previous.rssi != item.rssi ||
                previous.name != item.name ||
                previous.serviceUuids != item.serviceUuids

        seenDevices[address] = item

        if (changed) {
            _scanResults.tryEmit(item)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(filterByServiceUuid: Boolean = false) {
        if (!hasScanPermission()) {
            _status.value = "Missing BLE scan permission"
            Log.w(TAG, "startScan aborted: missing scan permission")
            return
        }

        if (!adapter.isEnabled) {
            _status.value = "Bluetooth is disabled"
            Log.w(TAG, "startScan aborted: Bluetooth disabled")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _status.value = "Bluetooth LE scanner unavailable"
            Log.w(TAG, "startScan aborted: scanner is null")
            return
        }

        stopScan()
        seenDevices.clear()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        if (filterByServiceUuid) {
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
            )
            scanner.startScan(filters, settings, scanCallback)
            _status.value = "Scanning for matching BLE service..."
            Log.d(TAG, "Started filtered BLE scan for service=$SERVICE_UUID")
        } else {
            scanner.startScan(null, settings, scanCallback)
            _status.value = "Scanning for BLE devices..."
            Log.d(TAG, "Started unfiltered BLE scan")
        }

        _isScanning.value = true
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BleScanItem) {
        if (!hasConnectPermission()) {
            _status.value = "Missing BLE connect permission"
            Log.w(TAG, "connect aborted: missing connect permission")
            return
        }

        stopScan()
        disconnect()

        _status.value = "Connecting to ${safeDeviceName(device)}..."
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.device.connectGatt(context, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        pendingNotificationUuids.clear()

        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
            } catch (_: Exception) {
            }

            try {
                gatt.close()
            } catch (_: Exception) {
            }
        }

        bluetoothGatt = null
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        _deviceInfo.value = null
        _status.value = ""
        _claimCode.value = ""

        pendingCommandWrites.clear()
        commandWriteInFlight = false
    }



    private val gattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid != CHAR_COMMAND_UUID) return

            commandWriteInFlight = false

            if (status != BluetoothGatt.GATT_SUCCESS) {
                pendingCommandWrites.clear()
                _status.value = "Command write failed: $status"
                Log.w(TAG, "Command write failed uuid=${characteristic.uuid} status=$status")
                return
            }

            drainCommandQueue(gatt)
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange status=$status newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _status.value = "Connection error: $status"
                safeCloseGatt(gatt)
                return
            }

            _connectionState.value = newState

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    bluetoothGatt = gatt
                    _status.value = "Connected. Discovering services..."
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    _status.value = "Disconnected"
                    safeCloseGatt(gatt)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "onServicesDiscovered status=$status")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _status.value = "Service discovery failed: $status"
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _status.value = "Expected BLE service not found"
                Log.w(TAG, "Service not found: $SERVICE_UUID")
                return
            }

            _status.value = "Services ready"
            queueNotificationSetup(gatt)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Characteristic read failed uuid=${characteristic.uuid} status=$status")
                return
            }

            val value = characteristic.value?.decodeToString().orEmpty()

            when (characteristic.uuid) {
                CHAR_DEVICE_INFO_UUID -> {
                    Log.d(TAG, "Device info: $value")
                    _deviceInfo.value = parseDeviceInfo(value)
                }

                CHAR_STATUS_UUID -> {
                    Log.d(TAG, "Status read: $value")
                    handleStatusFrame(value)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value?.decodeToString().orEmpty()

            when (characteristic.uuid) {
                CHAR_STATUS_UUID -> {
                    Log.d(TAG, "Status changed: $value")
                    handleStatusFrame(value)
                }

                CHAR_WIFI_SCAN_UUID -> {
                    Log.d(TAG, "Wi-Fi scan frame: $value")
                    handleWifiScanFrame(value)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(TAG, "onDescriptorWrite uuid=${descriptor.characteristic.uuid} status=$status")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                _status.value = "Notification setup failed: $status"
                return
            }

            if (pendingNotificationUuids.isNotEmpty()) {
                enableNextNotification(gatt)
            } else {
                readStatus(gatt)
                readDeviceInfo(gatt)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun queueNotificationSetup(gatt: BluetoothGatt) {
        pendingNotificationUuids.clear()
        pendingNotificationUuids.add(CHAR_STATUS_UUID)
        pendingNotificationUuids.add(CHAR_WIFI_SCAN_UUID)
        enableNextNotification(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun enableNextNotification(gatt: BluetoothGatt) {
        if (pendingNotificationUuids.isEmpty()) return

        val service = gatt.getService(SERVICE_UUID) ?: run {
            _status.value = "Service unavailable during notification setup"
            return
        }

        val charUuid = pendingNotificationUuids.removeFirst()
        val characteristic = service.getCharacteristic(charUuid)

        if (characteristic == null) {
            Log.w(TAG, "Characteristic not found for notification: $charUuid")
            enableNextNotification(gatt)
            return
        }

        val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!notificationEnabled) {
            Log.w(TAG, "setCharacteristicNotification failed for $charUuid")
            enableNextNotification(gatt)
            return
        }

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.w(TAG, "CCCD not found for $charUuid")
            enableNextNotification(gatt)
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val started = gatt.writeDescriptor(descriptor)

        if (!started) {
            Log.w(TAG, "writeDescriptor failed to start for $charUuid")
            enableNextNotification(gatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readDeviceInfo(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID) ?: run {
            _status.value = "Service unavailable"
            return
        }

        val characteristic = service.getCharacteristic(CHAR_DEVICE_INFO_UUID) ?: run {
            _status.value = "Device info characteristic unavailable"
            return
        }

        val started = gatt.readCharacteristic(characteristic)
        if (!started) {
            _status.value = "Failed to read device info"
        }
    }

    @SuppressLint("MissingPermission")
    fun readStatus() {
        val gatt = bluetoothGatt ?: run {
            _status.value = "Not connected"
            return
        }

        readStatus(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun readStatus(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID) ?: run {
            _status.value = "Service unavailable"
            return
        }

        val characteristic = service.getCharacteristic(CHAR_STATUS_UUID) ?: run {
            _status.value = "Status characteristic unavailable"
            return
        }

        val started = gatt.readCharacteristic(characteristic)
        if (!started) {
            _status.value = "Failed to read status"
            Log.w(TAG, "readCharacteristic failed to start for status")
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        val gatt = bluetoothGatt ?: run {
            _status.value = "Not connected"
            return
        }

        pendingCommandWrites.addLast(command)
        drainCommandQueue(gatt)
    }

    fun scanWifi() {
        sendCommand("""{"cmd":"scan_wifi"}""")
    }

    fun setWifi(ssid: String, pass: String) {
        val payload = json.encodeToString(
            BleCommand(
                cmd = "set_wifi",
                ssid = ssid,
                password = pass
            )
        )
        sendCommand(payload)
    }

    fun requestClaimCode() {
        sendCommand("""{"cmd":"code"}""")
    }




    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun safeCloseGatt(gatt: BluetoothGatt) {
        try {
            gatt.close()
        } catch (_: Exception) {
        }

        if (bluetoothGatt == gatt) {
            bluetoothGatt = null
        }

        pendingNotificationUuids.clear()
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        _deviceInfo.value = null
        _claimCode.value = ""
    }

    private fun safeDeviceName(device: BleScanItem): String {
        return try {
            device.name ?: device.device.name ?: "Unknown device"
        } catch (_: SecurityException) {
            device.address ?: "Unknown device"
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun handleStatusFrame(frame: String) {
        val text = frame.trim()
        _status.value = text

        if (text.startsWith("claim:")) {
            _claimCode.value = text.removePrefix("claim:").trim()
            Log.d(TAG, "Claim code updated: ${_claimCode.value}")
        } else {
            _claimCode.value = ""
        }
    }

    private fun handleWifiScanFrame(frame: String) {
        val parts = frame.split("|")
        if (parts.isEmpty()) return

        when (parts[0]) {
            "sr" -> {
                // sr|<index>|<rssi>|<secure>
                if (parts.size < 4) {
                    Log.w(TAG, "Invalid sr frame: $frame")
                    return
                }

                val index = parts[1].toIntOrNull() ?: return
                val rssi = parts[2].toIntOrNull() ?: return
                val secure = when (parts[3]) {
                    "1", "true", "yes" -> true
                    else -> false
                }

                pendingWifiNetworks[index] = PendingWifiNetwork(
                    index = index,
                    rssi = rssi,
                    secure = secure
                )
            }

            "sn" -> {
                // sn|<index>|<part>|<ssid-chunk>
                if (parts.size < 4) {
                    Log.w(TAG, "Invalid sn frame: $frame")
                    return
                }

                val index = parts[1].toIntOrNull() ?: return
                val partIndex = parts[2].toIntOrNull() ?: return

                // keep the rest joined in case chunk itself contains '|'
                val chunk = parts.drop(3).joinToString("|")

                val pending = pendingWifiNetworks[index]
                if (pending == null) {
                    Log.w(TAG, "sn frame without sr frame: $frame")
                    return
                }

                pending.ssidParts[partIndex] = chunk
            }

            "se" -> {
                // se|<index>
                if (parts.size < 2) {
                    Log.w(TAG, "Invalid se frame: $frame")
                    return
                }

                val index = parts[1].toIntOrNull() ?: return
                val pending = pendingWifiNetworks.remove(index)

                if (pending == null) {
                    Log.w(TAG, "se frame without pending record: $frame")
                    return
                }

                val ssid = pending.ssidParts
                    .toSortedMap()
                    .values
                    .joinToString(separator = "")
                    .trim()

                if (ssid.isBlank()) {
                    Log.w(TAG, "Completed Wi-Fi record with blank SSID for index=$index")
                    return
                }

                _wifiNetworks.tryEmit(
                    WifiScanResult(
                        ssid = ssid,
                        rssi = pending.rssi,
                        secure = pending.secure
                    )
                )
            }

            else -> {
                Log.w(TAG, "Unknown Wi-Fi scan frame: $frame")
            }
        }
    }

    private fun parseDeviceInfo(raw: String): BleDeviceInfo {
        return try {
            val map = raw
                .split(",")
                .mapNotNull { token ->
                    val parts = token.split("=", limit = 2)
                    if (parts.size == 2) {
                        parts[0].trim() to parts[1].trim()
                    } else {
                        null
                    }
                }
                .toMap()

            BleDeviceInfo(
                device_id = map["d"].orEmpty(),
                wifi_connected = map["w"] == "1",
                ssid = map["s"].orEmpty(),
                state = map["st"].orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse compact device info: $raw", e)
            BleDeviceInfo()
        }
    }

    @SuppressLint("MissingPermission")
    private fun drainCommandQueue(gatt: BluetoothGatt) {
        if (commandWriteInFlight || pendingCommandWrites.isEmpty()) return

        val service = gatt.getService(SERVICE_UUID) ?: run {
            _status.value = "BLE service unavailable"
            return
        }

        val characteristic = service.getCharacteristic(CHAR_COMMAND_UUID) ?: run {
            _status.value = "Command characteristic unavailable"
            return
        }

        val command = pendingCommandWrites.removeFirst()
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        characteristic.value = command.toByteArray(Charsets.UTF_8)

        commandWriteInFlight = gatt.writeCharacteristic(characteristic)
        if (!commandWriteInFlight) {
            pendingCommandWrites.addFirst(command)
            _status.value = "Failed to send command"
            Log.w(TAG, "writeCharacteristic failed to start")
        } else {
            Log.d(TAG, "Command sent: $command")
        }
    }




    fun acknowledgeClaimCode() {
        sendCommand("""{"cmd":"ack"}""")
    }

    fun beginMqttProvisioning() {
        sendCommand("pv|b")
    }

    fun commitMqttProvisioning() {
        sendCommand("pv|c")
    }

    fun sendMqttProvisioning(payload: MqttProvisioningPayload) {
        beginMqttProvisioning()
        sendProvisioningField("h", payload.host)
        sendProvisioningField("i", payload.clientId)
        sendProvisioningField("u", payload.username)
        sendProvisioningField("p", payload.password)
        sendCommand("pv|o|${payload.port}")
    }

    private fun sendProvisioningField(field: String, value: String) {
        if (value.isBlank()) return

        for (chunk in chunkUtf8Safe(value, 15)) {
            sendCommand("pv|$field|$chunk")
        }
    }

    private fun chunkUtf8Safe(value: String, maxBytes: Int): List<String> {
        if (value.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < value.length) {
            var end = start
            var byteCount = 0

            while (end < value.length) {
                val charBytes = value[end].toString().toByteArray(Charsets.UTF_8).size
                if (byteCount + charBytes > maxBytes) break
                byteCount += charBytes
                end++
            }

            if (end == start) {
                end = (start + 1).coerceAtMost(value.length)
            }

            chunks.add(value.substring(start, end))
            start = end
        }

        return chunks
    }
}
