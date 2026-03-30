package com.wntechs.tankcontroller.data.local
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "water_tank_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val baseUrl = stringPreferencesKey("mqtt_broker_url")
        val deviceId = stringPreferencesKey("mqtt_device_id")
    }

    // This flow now only emits settings relevant to the MQTT architecture
    val settingsFlow: Flow<DeviceConnectionSettings> = context.dataStore.data.map { prefs ->
        DeviceConnectionSettings(
            // Defaulting to your specific IP as requested earlier
            baseUrl = prefs[Keys.baseUrl] ?: "192.46.215.185",
            deviceId = prefs[Keys.deviceId] ?: "relay1"
        )
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.baseUrl] = url
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.deviceId] = deviceId
        }
    }

    // Optional: Single function to save both at once if needed
    suspend fun saveSettings(url: String, deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.baseUrl] = url
            prefs[Keys.deviceId] = deviceId
        }
    }
}

/**
 * Data class representing the MQTT connection parameters.
 */
data class DeviceConnectionSettings(
    val baseUrl: String,
    val deviceId: String,
)