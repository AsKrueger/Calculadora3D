package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.UnitType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for safe unit conversions within 3D Cost Manager.
 * Ensures dimensional compatibility and maintains high precision.
 */
public final class UnitConverter {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final int CALCULATION_SCALE = 8;

    private UnitConverter() {
        // Utility class
    }

    /**
     * Converts a value from one unit to another.
     * 
     * @param value The value to convert.
     * @param from  The source unit.
     * @param to    The target unit.
     * @return The converted value with scale 8.
     * @throws IllegalArgumentException if units are dimensionally incompatible.
     */
    public static BigDecimal convert(BigDecimal value, UnitType from, UnitType to) {
        if (value == null || from == null || to == null) {
            throw new IllegalArgumentException("Value and units must not be null");
        }

        if (from == to) {
            return value.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
        }

        // Mass: G <-> KG
        if (isMass(from) && isMass(to)) {
            if (from == UnitType.G && to == UnitType.KG) {
                return value.divide(THOUSAND, CALCULATION_SCALE, RoundingMode.HALF_UP);
            } else if (from == UnitType.KG && to == UnitType.G) {
                return value.multiply(THOUSAND).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
            }
        }

        // Note: L is not yet in UnitType, so ML <-> L is not implementable with current UnitType.
        // If a project uses ML, it must match the Material unit ML for now.

        throw new IllegalArgumentException(String.format("Incompatible units or conversion not supported for V1: %s to %s", from, to));
    }

    private static boolean isMass(UnitType unit) {
        return unit == UnitType.G || unit == UnitType.KG;
    }
}
