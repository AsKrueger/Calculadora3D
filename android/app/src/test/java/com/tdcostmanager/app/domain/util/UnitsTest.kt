package com.tdcostmanager.app.domain.util

import com.tdcostmanager.app.domain.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun sameDimensionWithoutDensity() {
        assertEquals(listOf(UnitType.G, UnitType.KG), compatibleUnits(UnitType.KG, null))
        assertEquals(listOf(UnitType.ML, UnitType.L), compatibleUnits(UnitType.L, null))
        assertEquals(listOf(UnitType.UNIT), compatibleUnits(UnitType.UNIT, 1.2))
    }

    @Test
    fun densityAllowsCrossingBetweenMassAndVolume() {
        assertEquals(listOf(UnitType.ML, UnitType.L, UnitType.G, UnitType.KG), compatibleUnits(UnitType.L, 1.1))
        assertEquals(listOf(UnitType.G, UnitType.KG), compatibleUnits(UnitType.G, 0.0))
    }
}
