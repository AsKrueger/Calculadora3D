package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.MaterialApi
import com.tdcostmanager.app.data.remote.dto.material.MaterialCreateRequest
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.data.remote.dto.material.MaterialUpdateRequest
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class MaterialRepository(
    private val api: MaterialApi = NetworkConfig.materialApi
) {
    suspend fun getMaterials(): Result<List<MaterialResponse>> {
        return try {
            Result.success(api.getMaterials())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMaterial(id: Long): Result<MaterialResponse> {
        return try {
            Result.success(api.getMaterial(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMaterial(request: MaterialCreateRequest): Result<MaterialResponse> {
        return try {
            Result.success(api.createMaterial(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMaterial(id: Long, request: MaterialUpdateRequest): Result<MaterialResponse> {
        return try {
            Result.success(api.updateMaterial(id, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateMaterial(id: Long): Result<Unit> {
        return try {
            api.deactivateMaterial(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
