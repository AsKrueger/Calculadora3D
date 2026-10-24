package com.tdcostmanager.app.data.remote.dto.quote

import com.tdcostmanager.app.domain.model.QuoteStatus
import kotlinx.serialization.Serializable

@Serializable
data class QuoteCreateRequest(
    val marginPercentage: Double,
    val safetyPercentage: Double,
    val calculationDateTime: String
)

@Serializable
data class QuoteStatusUpdateRequest(
    val status: QuoteStatus
)

// El desglose es null en los presupuestos generados antes de que el backend lo guardase
@Serializable
data class QuoteResponse(
    val id: Long,
    val projectId: Long,
    val marginPercentage: Double,
    val safetyPercentage: Double,
    val materialCost: Double? = null,
    val machineCost: Double? = null,
    val toolCost: Double? = null,
    val laborCost: Double? = null,
    val electricityCost: Double? = null,
    val energyKWh: Double? = null,
    val electricityPricePerKWh: Double? = null,
    val baseCost: Double,
    val safetyAmount: Double? = null,
    val adjustedCost: Double,
    val profitAmount: Double? = null,
    val finalPrice: Double,
    val status: QuoteStatus,
    val calculatedAt: String? = null,
    val createdAt: String
)
