package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import retrofit2.http.*

interface ProjectApi {
    @GET("api/v1/projects")
    suspend fun getProjects(): List<ProjectResponse>

    @POST("api/v1/projects")
    suspend fun createProject(@Body request: ProjectCreateRequest): ProjectResponse

    @GET("api/v1/projects/{id}")
    suspend fun getProject(@Path("id") id: Long): ProjectResponse

    @PUT("api/v1/projects/{id}")
    suspend fun updateProject(@Path("id") id: Long, @Body request: ProjectUpdateRequest): ProjectResponse

    @DELETE("api/v1/projects/{id}")
    suspend fun archiveProject(@Path("id") id: Long)
}
