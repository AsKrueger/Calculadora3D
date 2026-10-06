package com.tdcostmanager.app.data.remote.dto.material

import com.tdcostmanager.app.domain.model.MaterialCategory
import com.tdcostmanager.app.domain.model.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class MaterialResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val purchasePrice: Double,
    val quantity: Double,
    val unit: UnitType,
    val category: MaterialCategory,
    val active: Boolean
)

@Serializable
data class MaterialCreateRequest(
    val name: String,
    val description: String?,
    val purchasePrice: Double,
    val quantity: Double,
    val unit: UnitType,
    val category: MaterialCategory
)

@Serializable
data class MaterialUpdateRequest(
    val name: String,
    val description: String?,
    val purchasePrice: Double,
    val quantity: Double,
    val unit: UnitType,
    val category: MaterialCategory,
    val active: Boolean
)
