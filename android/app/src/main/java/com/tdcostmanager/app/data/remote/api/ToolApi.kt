package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.tool.ToolCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.data.remote.dto.tool.ToolUpdateRequest
import retrofit2.http.*

interface ToolApi {
    @GET("api/v1/tools")
    suspend fun getTools(): List<ToolResponse>

    @POST("api/v1/tools")
    suspend fun createTool(@Body request: ToolCreateRequest): ToolResponse

    @GET("api/v1/tools/{id}")
    suspend fun getTool(@Path("id") id: Long): ToolResponse

    @PUT("api/v1/tools/{id}")
    suspend fun updateTool(@Path("id") id: Long, @Body request: ToolUpdateRequest): ToolResponse

    @DELETE("api/v1/tools/{id}")
    suspend fun deactivateTool(@Path("id") id: Long)
}
