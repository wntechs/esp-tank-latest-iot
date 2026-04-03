package com.wntechs.tankcontroller.data.repository

import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.model.LoginRequest
import com.wntechs.tankcontroller.data.model.MqttCredentials
import com.wntechs.tankcontroller.data.model.MqttCredentialsRequest
import com.wntechs.tankcontroller.data.model.MqttRefreshRequest
import com.wntechs.tankcontroller.data.model.PairingClaimRequest
import com.wntechs.tankcontroller.data.model.PairingResponse
import com.wntechs.tankcontroller.data.model.PairingStartRequest
import com.wntechs.tankcontroller.data.model.ProvisioningStatusResponse
import com.wntechs.tankcontroller.data.model.RegisterRequest
import com.wntechs.tankcontroller.data.model.ValidationErrorResponse
import com.wntechs.tankcontroller.data.remote.AuthApi
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.serialization.json.Json
import retrofit2.Response

class UserRepository(
    private val authApi: AuthApi,
    private val settingsStore: SettingsStore
) {
    val authState = settingsStore.authFlow
    val mqttCreds = settingsStore.mqttCredsFlow

    suspend fun register(request: RegisterRequest): AppResult<Unit> {
        return try {
            val response = authApi.register(request)
            handleAuthResponse(response)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun login(request: LoginRequest): AppResult<Unit> {
        return try {
            val response = authApi.login(request)
            handleAuthResponse(response)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun logout(): AppResult<Unit> {
        return try {
            val response = authApi.logout()
            if (response.isSuccessful) {
                settingsStore.clearAuth()
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Logout failed")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun startPairing(code: String): AppResult<PairingResponse> {
        return try {
            val response = authApi.startPairing(PairingStartRequest(code))
            if (response.isSuccessful) {
                AppResult.Success(response.body()!!)
            } else {
                AppResult.Error(parseError(response))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun claimDevice(token: String): AppResult<PairingResponse> {
        return try {
            val response = authApi.claimDevice(PairingClaimRequest(token))
            if (response.isSuccessful) {
                AppResult.Success(response.body()!!)
            } else {
                AppResult.Error(parseError(response))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getProvisioningStatus(uuid: String, token: String): AppResult<ProvisioningStatusResponse> {
        return try {
            val response = authApi.getProvisioningStatus(uuid, token)
            if (response.isSuccessful) {
                AppResult.Success(response.body()!!)
            } else {
                AppResult.Error(parseError(response))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getMqttCredentials(deviceUuid: String, deviceName: String): AppResult<MqttCredentials> {
        return try {
            val response = authApi.getMqttCredentials(MqttCredentialsRequest(deviceUuid, deviceName))
            if (response.isSuccessful) {
                val creds = response.body()?.data?.mqtt ?: return AppResult.Error("Empty response body")
                settingsStore.saveMqttCreds(creds)
                AppResult.Success(creds)
            } else {
                AppResult.Error(parseError(response))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun refreshMqttCredentials(deviceUuid: String, clientId: String): AppResult<MqttCredentials> {
        return try {
            val response = authApi.refreshMqttCredentials(MqttRefreshRequest(deviceUuid, clientId))
            if (response.isSuccessful) {
                val creds = response.body()?.data?.mqtt ?: return AppResult.Error("Empty response body")
                settingsStore.saveMqttCreds(creds)
                AppResult.Success(creds)
            } else {
                AppResult.Error(parseError(response))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun <T : com.wntechs.tankcontroller.data.model.AuthResponse> handleAuthResponse(response: Response<T>): AppResult<Unit> {
        if (response.isSuccessful) {
            val body = response.body() ?: return AppResult.Error("Empty response body")
            settingsStore.saveAuth(body.token, body.user.name, body.user.email)
            return AppResult.Success(Unit)
        } else {
            return AppResult.Error(parseError(response))
        }
    }

    private fun parseError(response: Response<*>): String {
        val errorBody = response.errorBody()?.string()
        return if (errorBody != null) {
            try {
                val errorRes = Json.decodeFromString<ValidationErrorResponse>(errorBody)
                errorRes.message + (errorRes.errors?.let { ": " + it.values.flatten().joinToString(", ") } ?: "")
            } catch (e: Exception) {
                "Error code: ${response.code()}"
            }
        } else {
            "Error code: ${response.code()}"
        }
    }
}
