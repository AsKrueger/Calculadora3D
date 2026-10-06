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
data class QuoteResponse(
    val id: Long,
    val projectId: Long,
    val marginPercentage: Double,
    val safetyPercentage: Double,
    val baseCost: Double,
    val adjustedCost: Double,
    val finalPrice: Double,
    val status: QuoteStatus,
    val createdAt: String
)
