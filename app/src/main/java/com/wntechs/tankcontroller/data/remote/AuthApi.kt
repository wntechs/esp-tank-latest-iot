package com.wntechs.tankcontroller.data.remote

import com.wntechs.tankcontroller.data.model.AuthResponse
import com.wntechs.tankcontroller.data.model.LoginRequest
import com.wntechs.tankcontroller.data.model.MessageResponse
import com.wntechs.tankcontroller.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @DELETE("auth/logout")
    suspend fun logout(): Response<MessageResponse>
}
