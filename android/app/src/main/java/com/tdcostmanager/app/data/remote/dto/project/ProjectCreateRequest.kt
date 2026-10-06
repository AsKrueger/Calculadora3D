package com.tdcostmanager.app.data.remote.dto.project

import com.tdcostmanager.app.domain.model.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class ProjectCreateRequest(
    val name: String,
    val description: String?,
    val laborHours: Double,
    val laborCostPerHour: Double,
    val materials: List<MaterialRequest> = emptyList(),
    val machines: List<MachineRequest> = emptyList(),
    val tools: List<ToolRequest> = emptyList()
) {
    @Serializable
    data class MaterialRequest(
        val materialId: Long,
        val quantityUsed: Double,
        val unit: UnitType
    )

    @Serializable
    data class MachineRequest(
        val machineId: Long,
        val estimatedHours: Double
    )

    @Serializable
    data class ToolRequest(
        val toolId: Long,
        val uses: Double
    )
}
