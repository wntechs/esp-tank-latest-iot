package com.wntechs.tankcontroller.data.remote

import com.wntechs.tankcontroller.data.model.AuthResponse
import com.wntechs.tankcontroller.data.model.DeviceListResponse
import com.wntechs.tankcontroller.data.model.LoginRequest
import com.wntechs.tankcontroller.data.model.MessageResponse
import com.wntechs.tankcontroller.data.model.MobileProvisioningRequest
import com.wntechs.tankcontroller.data.model.MobileProvisioningResponse
import com.wntechs.tankcontroller.data.model.MqttCredentialsRequest
import com.wntechs.tankcontroller.data.model.MqttCredentialsResponse
import com.wntechs.tankcontroller.data.model.MqttRefreshRequest
import com.wntechs.tankcontroller.data.model.PairingClaimRequest
import com.wntechs.tankcontroller.data.model.PairingResponse
import com.wntechs.tankcontroller.data.model.PairingStartRequest
import com.wntechs.tankcontroller.data.model.ProvisioningStatusResponse
import com.wntechs.tankcontroller.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @DELETE("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @POST("mqtt/mobile-credentials")
    suspend fun getMqttCredentials(@Body request: MqttCredentialsRequest): Response<MqttCredentialsResponse>

    @POST("mqtt/mobile-credentials/refresh")
    suspend fun refreshMqttCredentials(@Body request: MqttRefreshRequest): Response<MqttCredentialsResponse>

    @POST("pairing/start")
    suspend fun startPairing(@Body request: PairingStartRequest): Response<PairingResponse>

    @POST("pairing/claim")
    suspend fun claimDevice(@Body request: PairingClaimRequest): Response<PairingResponse>

    @GET("device/provisioning-status")
    suspend fun getProvisioningStatus(
        @Query("uuid") uuid: String,
        @Query("token") token: String
    ): Response<ProvisioningStatusResponse>

    @GET("devices")
    suspend fun getDevices(): Response<DeviceListResponse>

    @POST("devices/{uuid}/reset-request")
    suspend fun resetDevice(@Path("uuid") uuid: String, @Body body: Map<String, String> = emptyMap()): Response<MessageResponse>

    @POST("devices/provision/mobile")
    suspend fun provisionDeviceMobile(@Body request: MobileProvisioningRequest): Response<MobileProvisioningResponse>
}
