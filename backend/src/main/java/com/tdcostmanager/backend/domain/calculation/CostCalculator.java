package com.tdcostmanager.backend.domain.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pure domain logic for calculating project costs based on CostCalculationInput.
 * Precision: Internal calculation uses 8 decimals. Final result uses 4 decimals.
 */
public final class CostCalculator {

    private static final int INTERNAL_SCALE = 8;
    private static final int FINAL_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    private CostCalculator() {
        // Domain service logic
    }

    /**
     * Performs a full cost calculation for input data on a specific date and time.
     */
    public static CostCalculationResult calculate(
            CostCalculationInput input,
            LocalDateTime calculationDateTime,
            BigDecimal marginPercentage,
            BigDecimal safetyPercentage,
            ElectricityPriceProvider priceProvider) {

        validateInputs(input, calculationDateTime, marginPercentage, safetyPercentage, priceProvider);

        BigDecimal materialCost = calculateMaterialCost(input);
        BigDecimal machineCost = calculateMachineCost(input);
        BigDecimal toolCost = calculateToolCost(input);
        BigDecimal laborCost = calculateLaborCost(input);
        BigDecimal electricityCost = calculateElectricityCost(input, calculationDateTime, priceProvider);

        BigDecimal baseCost = materialCost
                .add(machineCost)
                .add(toolCost)
                .add(laborCost)
                .add(electricityCost);

        BigDecimal safetyAmount = baseCost.multiply(safetyPercentage)
                .divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal adjustedCost = baseCost.add(safetyAmount);

        BigDecimal profitAmount = adjustedCost.multiply(marginPercentage)
                .divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal finalPrice = adjustedCost.add(profitAmount);

        return new CostCalculationResult(
                scaleToFinal(materialCost),
                scaleToFinal(machineCost),
                scaleToFinal(toolCost),
                scaleToFinal(laborCost),
                scaleToFinal(electricityCost),
                scaleToFinal(baseCost),
                scaleToFinal(safetyAmount),
                scaleToFinal(adjustedCost),
                scaleToFinal(profitAmount),
                scaleToFinal(finalPrice)
        );
    }

    private static void validateInputs(CostCalculationInput input, LocalDateTime dateTime, BigDecimal margin, BigDecimal safety, ElectricityPriceProvider provider) {
        Objects.requireNonNull(input, "CostCalculationInput cannot be null");
        Objects.requireNonNull(dateTime, "Calculation date and time cannot be null");
        Objects.requireNonNull(margin, "Margin percentage cannot be null");
        Objects.requireNonNull(safety, "Safety percentage cannot be null");
        Objects.requireNonNull(provider, "Price provider cannot be null");

        if (margin.compareTo(BigDecimal.ZERO) < 0 || safety.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentages cannot be negative");
        }
    }

    private static BigDecimal calculateMaterialCost(CostCalculationInput input) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (CostCalculationInput.MaterialInput mat : input.materials()) {
            if (mat.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Material purchased quantity must be greater than zero for: " + mat.name());
            }

            BigDecimal unitPrice = mat.purchasePrice()
                    .divide(mat.quantity(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal normalizedQuantity = UnitConverter.convert(mat.quantityUsed(), mat.unit(), mat.materialUnit());
            
            BigDecimal cost = normalizedQuantity.multiply(unitPrice);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateMachineCost(CostCalculationInput input) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (CostCalculationInput.MachineInput mach : input.machines()) {
            if (mach.usefulLifeHours().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Machine useful life must be greater than zero for: " + mach.name());
            }

            BigDecimal hourlyAmortization = mach.acquisitionCost()
                    .divide(mach.usefulLifeHours(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal hourlyCost = hourlyAmortization.add(mach.maintenanceCostPerHour());
            
            BigDecimal cost = mach.estimatedHours().multiply(hourlyCost);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateToolCost(CostCalculationInput input) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (CostCalculationInput.ToolInput tool : input.tools()) {
            if (tool.estimatedUses().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tool estimated uses must be greater than zero for: " + tool.name());
            }

            BigDecimal costPerUse = tool.acquisitionCost()
                    .divide(tool.estimatedUses(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal maintenanceFactor = BigDecimal.ONE.add(
                    tool.maintenancePercentage().divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP)
            );

            BigDecimal totalCostPerUse = costPerUse.multiply(maintenanceFactor);
            
            BigDecimal cost = tool.uses().multiply(totalCostPerUse);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateLaborCost(CostCalculationInput input) {
        return input.laborHours().multiply(input.laborCostPerHour())
                .setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateElectricityCost(CostCalculationInput input, LocalDateTime calculationDateTime, ElectricityPriceProvider priceProvider) {
        BigDecimal totalKWh = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (CostCalculationInput.MachineInput mach : input.machines()) {
            BigDecimal powerKW = mach.powerWatts()
                    .divide(THOUSAND, INTERNAL_SCALE, RoundingMode.HALF_UP);
            
            BigDecimal energyKWh = powerKW.multiply(mach.estimatedHours());
            totalKWh = totalKWh.add(energyKWh);
        }

        BigDecimal price = priceProvider.getPricePerKWh(calculationDateTime);
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Electricity price cannot be null or negative");
        }

        return totalKWh.multiply(price).setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleToFinal(BigDecimal value) {
        return value.setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }
}
