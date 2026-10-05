package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.HealthApi
import com.tdcostmanager.app.data.remote.dto.HealthResponse
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class HealthRepository(
    private val api: HealthApi = NetworkConfig.healthApi
) {
    suspend fun getBackendHealth(): Result<HealthResponse> {
        return try {
            Result.success(api.getHealth())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
