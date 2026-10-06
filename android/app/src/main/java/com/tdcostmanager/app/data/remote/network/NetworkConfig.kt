package com.tdcostmanager.app.data.remote.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tdcostmanager.app.BuildConfig
import com.tdcostmanager.app.data.local.TokenManager
import com.tdcostmanager.app.data.remote.api.AuthApi
import com.tdcostmanager.app.data.remote.api.HealthApi
import com.tdcostmanager.app.data.remote.api.MachineApi
import com.tdcostmanager.app.data.remote.api.MaterialApi
import com.tdcostmanager.app.data.remote.api.ProjectApi
import com.tdcostmanager.app.data.remote.api.ToolApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkConfig {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var retrofit: Retrofit? = null

    fun initialize(context: Context) {
        if (retrofit != null) return

        val tokenManager = TokenManager(context)
        
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                // Evitamos Level.BODY para no loguear el JWT en las cabeceras
                HttpLoggingInterceptor.Level.BASIC 
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: throw IllegalStateException("NetworkConfig must be initialized first")
    }

    val healthApi: HealthApi by lazy {
        getRetrofit().create(HealthApi::class.java)
    }

    val authApi: AuthApi by lazy {
        getRetrofit().create(AuthApi::class.java)
    }

    val projectApi: ProjectApi by lazy {
        getRetrofit().create(ProjectApi::class.java)
    }

    val materialApi: MaterialApi by lazy {
        getRetrofit().create(MaterialApi::class.java)
    }

    val machineApi: MachineApi by lazy {
        getRetrofit().create(MachineApi::class.java)
    }

    val toolApi: ToolApi by lazy {
        getRetrofit().create(ToolApi::class.java)
    }
}
