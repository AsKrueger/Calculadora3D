package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.HealthResponse
import retrofit2.http.GET

interface HealthApi {
    @GET("api/v1/health")
    suspend fun getHealth(): HealthResponse
}
