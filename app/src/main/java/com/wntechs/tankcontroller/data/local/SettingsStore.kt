package com.wntechs.tankcontroller.data.local
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wntechs.tankcontroller.data.model.MqttCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "water_tank_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val baseUrl = stringPreferencesKey("mqtt_broker_url")
        val deviceId = stringPreferencesKey("mqtt_device_id")
        val authToken = stringPreferencesKey("auth_token")
        val userName = stringPreferencesKey("user_name")
        val userEmail = stringPreferencesKey("user_email")
        val mqttCreds = stringPreferencesKey("mqtt_creds")
    }

    val settingsFlow: Flow<DeviceConnectionSettings> = context.dataStore.data.map { prefs ->
        DeviceConnectionSettings(
            baseUrl = prefs[Keys.baseUrl] ?: "192.46.215.185",
            deviceId = prefs[Keys.deviceId] ?: ""
        )
    }

    val authFlow: Flow<AuthState> = context.dataStore.data.map { prefs ->
        AuthState(
            token = prefs[Keys.authToken],
            name = prefs[Keys.userName],
            email = prefs[Keys.userEmail]
        )
    }

    val mqttCredsFlow: Flow<MqttCredentials?> = context.dataStore.data.map { prefs ->
        prefs[Keys.mqttCreds]?.let { 
            try { Json.decodeFromString<MqttCredentials>(it) } catch (e: Exception) { null }
        }
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

    suspend fun saveAuth(token: String, name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.authToken] = token
            prefs[Keys.userName] = name
            prefs[Keys.userEmail] = email
        }
    }

    suspend fun saveMqttCreds(creds: MqttCredentials) {
        context.dataStore.edit { prefs ->
            prefs[Keys.mqttCreds] = Json.encodeToString(creds)
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.authToken)
            prefs.remove(Keys.userName)
            prefs.remove(Keys.userEmail)
            prefs.remove(Keys.mqttCreds)
            prefs.remove(Keys.deviceId)
        }
    }
}

data class DeviceConnectionSettings(
    val baseUrl: String,
    val deviceId: String,
)

data class AuthState(
    val token: String?,
    val name: String?,
    val email: String?
) {
    val isLoggedIn: Boolean get() = token != null
}
