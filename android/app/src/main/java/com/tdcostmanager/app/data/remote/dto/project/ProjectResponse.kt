package com.tdcostmanager.app.data.remote.dto.project

import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.domain.model.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val status: ProjectStatus,
    val laborHours: Double,
    val laborCostPerHour: Double,
    val materials: List<MaterialItem>,
    val machines: List<MachineItem>,
    val tools: List<ToolItem>,
    val createdAt: String,
    val updatedAt: String
) {
    @Serializable
    data class MaterialItem(
        val materialId: Long,
        val name: String,
        val quantityUsed: Double,
        val unit: UnitType
    )

    @Serializable
    data class MachineItem(
        val machineId: Long,
        val name: String,
        val estimatedHours: Double
    )

    @Serializable
    data class ToolItem(
        val toolId: Long,
        val name: String,
        val uses: Double
    )
}
