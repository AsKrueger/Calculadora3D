package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.ToolApi
import com.tdcostmanager.app.data.remote.dto.tool.ToolCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.data.remote.dto.tool.ToolUpdateRequest
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class ToolRepository(
    private val api: ToolApi = NetworkConfig.toolApi
) {
    suspend fun getTools(): Result<List<ToolResponse>> {
        return try {
            Result.success(api.getTools())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTool(id: Long): Result<ToolResponse> {
        return try {
            Result.success(api.getTool(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTool(request: ToolCreateRequest): Result<ToolResponse> {
        return try {
            Result.success(api.createTool(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTool(id: Long, request: ToolUpdateRequest): Result<ToolResponse> {
        return try {
            Result.success(api.updateTool(id, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateTool(id: Long): Result<Unit> {
        return try {
            api.deactivateTool(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
