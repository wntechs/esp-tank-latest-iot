package com.wntechs.tankcontroller.data.repository

import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.model.LoginRequest
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

    private suspend fun handleAuthResponse(response: Response<com.wntechs.tankcontroller.data.model.AuthResponse>): AppResult<Unit> {
        if (response.isSuccessful) {
            val body = response.body() ?: return AppResult.Error("Empty response body")
            settingsStore.saveAuth(body.token, body.user.name, body.user.email)
            return AppResult.Success(Unit)
        } else {
            val errorBody = response.errorBody()?.string()
            val message = if (errorBody != null) {
                try {
                    val errorRes = Json.decodeFromString<ValidationErrorResponse>(errorBody)
                    errorRes.message + (errorRes.errors?.let { ": " + it.values.flatten().joinToString(", ") } ?: "")
                } catch (e: Exception) {
                    "Error code: ${response.code()}"
                }
            } else {
                "Error code: ${response.code()}"
            }
            return AppResult.Error(message)
        }
    }
}
