package com.tdcostmanager.app.data.remote.dto.machine

import kotlinx.serialization.Serializable

@Serializable
data class MachineResponse(
    val id: Long,
    val name: String,
    val acquisitionCost: Double,
    val usefulLifeHours: Double,
    val powerWatts: Double,
    val maintenanceCostPerHour: Double,
    val active: Boolean
)

@Serializable
data class MachineCreateRequest(
    val name: String,
    val acquisitionCost: Double,
    val usefulLifeHours: Double,
    val powerWatts: Double,
    val maintenanceCostPerHour: Double
)

@Serializable
data class MachineUpdateRequest(
    val name: String,
    val acquisitionCost: Double,
    val usefulLifeHours: Double,
    val powerWatts: Double,
    val maintenanceCostPerHour: Double,
    val active: Boolean
)
