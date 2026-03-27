package com.wntechs.tankcontroller.data.remote

import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import com.wntechs.tankcontroller.data.model.RelayModeResponse
import com.wntechs.tankcontroller.data.model.ManualRelayRequest
import com.wntechs.tankcontroller.data.model.StatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DeviceApi {
    @GET(".")
    suspend fun root(): String

    @GET("status")
    suspend fun getStatus(): StatusResponse

    @GET("config")
    suspend fun getConfig(): ConfigResponse

    @POST("config")
    suspend fun updateConfig(@Body body: ConfigUpdateRequest): RelayModeResponse

    @POST("relay/manual")
    suspend fun setManualRelay(@Body body: ManualRelayRequest): RelayModeResponse

    @POST("relay/auto")
    suspend fun setAutoMode(): RelayModeResponse
}
