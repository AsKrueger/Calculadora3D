package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.machine.MachineCreateRequest
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.data.remote.dto.machine.MachineUpdateRequest
import retrofit2.http.*

interface MachineApi {
    @GET("api/v1/machines")
    suspend fun getMachines(): List<MachineResponse>

    @POST("api/v1/machines")
    suspend fun createMachine(@Body request: MachineCreateRequest): MachineResponse

    @GET("api/v1/machines/{id}")
    suspend fun getMachine(@Path("id") id: Long): MachineResponse

    @PUT("api/v1/machines/{id}")
    suspend fun updateMachine(@Path("id") id: Long, @Body request: MachineUpdateRequest): MachineResponse

    @DELETE("api/v1/machines/{id}")
    suspend fun deactivateMachine(@Path("id") id: Long)
}
