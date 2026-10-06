package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.ProjectApi
import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import com.tdcostmanager.app.data.remote.network.NetworkConfig
import retrofit2.HttpException

class ProjectRepository(
    private val api: ProjectApi = NetworkConfig.projectApi
) {
    suspend fun getProjects(): Result<List<ProjectResponse>> {
        return try {
            Result.success(api.getProjects())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProject(id: Long): Result<ProjectResponse> {
        return try {
            Result.success(api.getProject(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProject(request: ProjectCreateRequest): Result<ProjectResponse> {
        return try {
            Result.success(api.createProject(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(id: Long, request: ProjectUpdateRequest): Result<ProjectResponse> {
        return try {
            Result.success(api.updateProject(id, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveProject(id: Long): Result<Unit> {
        return try {
            api.archiveProject(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
