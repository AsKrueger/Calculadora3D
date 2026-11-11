package com.tdcostmanager.app.data.remote.dto.project

import com.tdcostmanager.app.domain.model.ProjectStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProjectUpdateRequest(
    val name: String,
    val description: String?,
    val status: ProjectStatus,
    val laborHours: Double,
    val laborCostPerHour: Double,
    // null: esas líneas no cambian; una lista (aunque esté vacía) sustituye a las actuales
    val materials: List<ProjectCreateRequest.MaterialRequest>? = null,
    val machines: List<ProjectCreateRequest.MachineRequest>? = null,
    val tools: List<ProjectCreateRequest.ToolRequest>? = null
)
