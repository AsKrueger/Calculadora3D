package com.tdcostmanager.app.data.remote.dto.project

import com.tdcostmanager.app.domain.model.ProjectStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProjectUpdateRequest(
    val name: String,
    val description: String?,
    val status: ProjectStatus,
    val laborHours: Double,
    val laborCostPerHour: Double
)
