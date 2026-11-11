package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.UnitType;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for safe unit conversions within 3D Cost Manager.
 * Ensures dimensional compatibility and maintains high precision.
 * Mass and volume can be converted into each other only when the material density (g/ml) is known.
 */
public final class UnitConverter {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final int CALCULATION_SCALE = 8;

    private UnitConverter() {
        // Utility class
    }

    /**
     * Converts a value from one unit to another without density (same dimension only).
     *
     * @throws IllegalArgumentException if units are dimensionally incompatible.
     */
    public static BigDecimal convert(BigDecimal value, UnitType from, UnitType to) {
        return convert(value, from, to, null);
    }

    /**
     * Converts a value from one unit to another.
     *
     * @param value          The value to convert.
     * @param from           The source unit.
     * @param to             The target unit.
     * @param densityGPerMl  Density of the material in g/ml, required only to convert between mass and volume.
     * @return The converted value with scale 8.
     * @throws IllegalArgumentException if units are incompatible or the density is missing.
     */
    public static BigDecimal convert(BigDecimal value, UnitType from, UnitType to, BigDecimal densityGPerMl) {
        if (value == null || from == null || to == null) {
            throw new IllegalArgumentException("Value and units must not be null");
        }
        if (!canConvert(from, to, densityGPerMl)) {
            throw new IllegalArgumentException(incompatibleMessage(from, to));
        }
        if (from == to) {
            return value.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
        }

        // Se pasa por la unidad base de su dimensión (gramos o mililitros) y, si hace falta, se cruza con la densidad
        BigDecimal base = toBase(value, from);
        if (isMass(from) && isVolume(to)) {
            base = base.divide(densityGPerMl, CALCULATION_SCALE, RoundingMode.HALF_UP);
        } else if (isVolume(from) && isMass(to)) {
            base = base.multiply(densityGPerMl);
        }
        return fromBase(base, to).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /** True si la conversión es posible con los datos disponibles. */
    public static boolean canConvert(UnitType from, UnitType to, BigDecimal densityGPerMl) {
        if (from == to) return true;
        if (isMass(from) && isMass(to)) return true;
        if (isVolume(from) && isVolume(to)) return true;
        boolean crossesDimension = (isMass(from) && isVolume(to)) || (isVolume(from) && isMass(to));
        return crossesDimension && densityGPerMl != null && densityGPerMl.signum() > 0;
    }

    public static String incompatibleMessage(UnitType from, UnitType to) {
        boolean crossesDimension = (isMass(from) && isVolume(to)) || (isVolume(from) && isMass(to));
        return crossesDimension
                ? String.format("Para convertir %s a %s el material necesita una densidad (g/ml)", symbol(from), symbol(to))
                : String.format("No se puede convertir %s a %s", symbol(from), symbol(to));
    }

    private static String symbol(UnitType unit) {
        return switch (unit) {
            case G -> "g";
            case KG -> "kg";
            case ML -> "ml";
            case L -> "L";
            case UNIT -> "unidades";
            case H -> "horas";
        };
    }

    private static BigDecimal toBase(BigDecimal value, UnitType unit) {
        return (unit == UnitType.KG || unit == UnitType.L) ? value.multiply(THOUSAND) : value;
    }

    private static BigDecimal fromBase(BigDecimal value, UnitType unit) {
        return (unit == UnitType.KG || unit == UnitType.L)
                ? value.divide(THOUSAND, CALCULATION_SCALE, RoundingMode.HALF_UP)
                : value;
    }

    private static boolean isMass(UnitType unit) {
        return unit == UnitType.G || unit == UnitType.KG;
    }

    private static boolean isVolume(UnitType unit) {
        return unit == UnitType.ML || unit == UnitType.L;
    }
}
