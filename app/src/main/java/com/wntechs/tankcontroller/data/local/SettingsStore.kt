package com.wntechs.tankcontroller.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "water_tank_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val hostName = stringPreferencesKey("host_name")
        val ipAddress = stringPreferencesKey("ip_address")
        val preferMdns = booleanPreferencesKey("prefer_mdns")
    }

    val settingsFlow: Flow<DeviceConnectionSettings> = context.dataStore.data.map { prefs ->
        DeviceConnectionSettings(
            baseUrl = prefs[Keys.baseUrl].orEmpty(),
            hostName = prefs[Keys.hostName].orEmpty(),
            ipAddress = prefs[Keys.ipAddress].orEmpty(),
            preferMdns = prefs[Keys.preferMdns] ?: true,
        )
    }

    suspend fun saveConnection(baseUrl: String, hostName: String = "", ipAddress: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.baseUrl] = baseUrl
            prefs[Keys.hostName] = hostName
            prefs[Keys.ipAddress] = ipAddress
        }
    }

    suspend fun setPreferMdns(preferMdns: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.preferMdns] = preferMdns
        }
    }
}

data class DeviceConnectionSettings(
    val baseUrl: String,
    val hostName: String,
    val ipAddress: String,
    val preferMdns: Boolean,
)
