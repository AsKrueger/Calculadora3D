package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.MachineApi
import com.tdcostmanager.app.data.remote.dto.machine.MachineCreateRequest
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.data.remote.dto.machine.MachineUpdateRequest
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class MachineRepository(
    private val api: MachineApi = NetworkConfig.machineApi
) {
    suspend fun getMachines(): Result<List<MachineResponse>> {
        return try {
            Result.success(api.getMachines())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMachine(id: Long): Result<MachineResponse> {
        return try {
            Result.success(api.getMachine(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMachine(request: MachineCreateRequest): Result<MachineResponse> {
        return try {
            Result.success(api.createMachine(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMachine(id: Long, request: MachineUpdateRequest): Result<MachineResponse> {
        return try {
            Result.success(api.updateMachine(id, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateMachine(id: Long): Result<Unit> {
        return try {
            api.deactivateMachine(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
