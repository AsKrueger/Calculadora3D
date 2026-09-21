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

        // Volume: ML <-> L
        if (isVolume(from) && isVolume(to)) {
            if (from == UnitType.ML && to == UnitType.L) {
                return value.divide(THOUSAND, CALCULATION_SCALE, RoundingMode.HALF_UP);
            } else if (from == UnitType.L && to == UnitType.ML) {
                return value.multiply(THOUSAND).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
            }
        }

        throw new IllegalArgumentException(String.format("Incompatible units or conversion not supported: %s to %s", from, to));
    }

    private static boolean isMass(UnitType unit) {
        return unit == UnitType.G || unit == UnitType.KG;
    }

    private static boolean isVolume(UnitType unit) {
        return unit == UnitType.ML || unit == UnitType.L;
    }
}
