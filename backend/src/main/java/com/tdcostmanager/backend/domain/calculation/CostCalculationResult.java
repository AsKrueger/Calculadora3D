package com.tdcostmanager.backend.domain.calculation;

import java.math.BigDecimal;

/**
 * Domain result object representing the breakdown of a cost calculation.
 * All values are stored with scale 4 for persistence/output.
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
    BigDecimal finalPrice
) {
}
