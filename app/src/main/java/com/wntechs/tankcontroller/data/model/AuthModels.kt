package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("device_name") val deviceName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String
)

@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("token_type") val tokenType: String,
    val user: AuthUser
)

@Serializable
data class AuthUser(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>? = null
)
