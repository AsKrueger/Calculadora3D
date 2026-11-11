package com.tdcostmanager.app.domain.util

import com.tdcostmanager.app.domain.model.UnitType

private val mass = listOf(UnitType.G, UnitType.KG)
private val volume = listOf(UnitType.ML, UnitType.L)

/**
 * Unidades en las que se puede usar un material comprado en [purchaseUnit]: las de su misma dimensión
 * y, si se conoce su densidad, también las de la otra (masa ↔ volumen). Es la misma regla que aplica el backend.
 */
fun compatibleUnits(purchaseUnit: UnitType, densityGPerMl: Double?): List<UnitType> {
    val hasDensity = densityGPerMl != null && densityGPerMl > 0
    return when (purchaseUnit) {
        in mass -> if (hasDensity) mass + volume else mass
        in volume -> if (hasDensity) volume + mass else volume
        else -> listOf(purchaseUnit)
    }
}
