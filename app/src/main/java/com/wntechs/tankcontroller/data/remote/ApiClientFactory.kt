package com.wntechs.tankcontroller.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wntechs.tankcontroller.data.local.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class ApiClientFactory(
    private val settingsStore: SettingsStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val authInterceptor = Interceptor { chain ->
        val token = kotlinx.coroutines.runBlocking {
            settingsStore.authFlow.first().token
        }
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
        
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        chain.proceed(requestBuilder.build())
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val authRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://esp-tank.wabcloud.com/api/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun createAuthApi(): AuthApi = authRetrofit.create(AuthApi::class.java)

    suspend fun createDeviceApi(): DeviceApi {
        val settings = settingsStore.settingsFlow.first()
        val baseUrl = settings.baseUrl.ifBlank { "http://192.168.1.1/" }
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeviceApi::class.java)
    }

    private fun normalizeBaseUrl(url: String): String =
        if (url.endsWith('/')) url else "$url/"
}
