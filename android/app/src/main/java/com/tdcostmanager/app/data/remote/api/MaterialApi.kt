package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.material.MaterialCreateRequest
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.data.remote.dto.material.MaterialUpdateRequest
import retrofit2.http.*

interface MaterialApi {
    @GET("api/v1/materials")
    suspend fun getMaterials(): List<MaterialResponse>

    @POST("api/v1/materials")
    suspend fun createMaterial(@Body request: MaterialCreateRequest): MaterialResponse

    @GET("api/v1/materials/{id}")
    suspend fun getMaterial(@Path("id") id: Long): MaterialResponse

    @PUT("api/v1/materials/{id}")
    suspend fun updateMaterial(@Path("id") id: Long, @Body request: MaterialUpdateRequest): MaterialResponse

    @DELETE("api/v1/materials/{id}")
    suspend fun deactivateMaterial(@Path("id") id: Long)
}
