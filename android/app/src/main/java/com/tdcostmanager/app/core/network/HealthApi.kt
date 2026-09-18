package com.tdcostmanager.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class HealthResponse(
    val status: String
)

interface HealthApi {
    @GET("api/v1/health")
    suspend fun getHealth(): HealthResponse
}
