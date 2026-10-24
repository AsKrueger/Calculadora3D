package com.tdcostmanager.backend.domain.calculation;

import java.math.BigDecimal;

/**
 * Domain result object representing the breakdown of a cost calculation.
 * Monetary values and energy use scale 4; the electricity price keeps the provider's precision.
 * electricityPricePerKWh is null when no energy is consumed, because the provider is not consulted.
 */
public record CostCalculationResult(
    BigDecimal materialCost,
    BigDecimal machineCost,
    BigDecimal toolCost,
    BigDecimal laborCost,
    BigDecimal electricityCost,
    BigDecimal baseCost,
    BigDecimal safetyAmount,
    BigDecimal adjustedCost,
    BigDecimal profitAmount,
    BigDecimal finalPrice,
    BigDecimal energyKWh,
    BigDecimal electricityPricePerKWh
) {
}
