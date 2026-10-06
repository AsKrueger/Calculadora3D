package com.tdcostmanager.app.data.remote.dto.tool

import kotlinx.serialization.Serializable

@Serializable
data class ToolResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val acquisitionCost: Double,
    val estimatedUses: Double,
    val maintenancePercentage: Double,
    val active: Boolean
)

@Serializable
data class ToolCreateRequest(
    val name: String,
    val description: String?,
    val acquisitionCost: Double,
    val estimatedUses: Double,
    val maintenancePercentage: Double
)

@Serializable
data class ToolUpdateRequest(
    val name: String,
    val description: String?,
    val acquisitionCost: Double,
    val estimatedUses: Double,
    val maintenancePercentage: Double,
    val active: Boolean
)
