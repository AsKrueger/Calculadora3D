package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectMachine;
import com.tdcostmanager.backend.domain.model.ProjectMaterial;
import com.tdcostmanager.backend.domain.model.ProjectTool;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pure domain logic for calculating project costs.
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
     * Performs a full cost calculation for a project on a specific date and time.
     */
    public static CostCalculationResult calculate(
            Project project,
            LocalDateTime calculationDateTime,
            BigDecimal marginPercentage,
            BigDecimal safetyPercentage,
            ElectricityPriceProvider priceProvider) {

        validateInputs(project, calculationDateTime, marginPercentage, safetyPercentage, priceProvider);

        BigDecimal materialCost = calculateMaterialCost(project);
        BigDecimal machineCost = calculateMachineCost(project);
        BigDecimal toolCost = calculateToolCost(project);
        BigDecimal laborCost = calculateLaborCost(project);
        BigDecimal electricityCost = calculateElectricityCost(project, calculationDateTime, priceProvider);

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

    private static void validateInputs(Project project, LocalDateTime dateTime, BigDecimal margin, BigDecimal safety, ElectricityPriceProvider provider) {
        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(dateTime, "Calculation date and time cannot be null");
        Objects.requireNonNull(margin, "Margin percentage cannot be null");
        Objects.requireNonNull(safety, "Safety percentage cannot be null");
        Objects.requireNonNull(provider, "Price provider cannot be null");

        if (margin.compareTo(BigDecimal.ZERO) < 0 || safety.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentages cannot be negative");
        }
        if (project.getLaborHours().compareTo(BigDecimal.ZERO) < 0 || 
            project.getLaborCostPerHour().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Labor values cannot be negative");
        }
    }

    private static BigDecimal calculateMaterialCost(Project project) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (ProjectMaterial pm : project.getProjectMaterials()) {
            var material = pm.getMaterial();
            if (material.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Material purchased quantity must be greater than zero for: " + material.getName());
            }

            BigDecimal unitPrice = material.getPurchasePrice()
                    .divide(material.getQuantity(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal normalizedQuantity = UnitConverter.convert(pm.getQuantityUsed(), pm.getUnit(), material.getUnit());
            
            BigDecimal cost = normalizedQuantity.multiply(unitPrice);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateMachineCost(Project project) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (ProjectMachine pm : project.getProjectMachines()) {
            var machine = pm.getMachine();
            if (machine.getUsefulLifeHours().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Machine useful life must be greater than zero for: " + machine.getName());
            }

            BigDecimal hourlyAmortization = machine.getAcquisitionCost()
                    .divide(machine.getUsefulLifeHours(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal hourlyCost = hourlyAmortization.add(machine.getMaintenanceCostPerHour());
            
            BigDecimal cost = pm.getEstimatedHours().multiply(hourlyCost);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateToolCost(Project project) {
        BigDecimal total = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (ProjectTool pt : project.getProjectTools()) {
            var tool = pt.getTool();
            if (tool.getEstimatedUses().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tool estimated uses must be greater than zero for: " + tool.getName());
            }

            BigDecimal costPerUse = tool.getAcquisitionCost()
                    .divide(tool.getEstimatedUses(), INTERNAL_SCALE, RoundingMode.HALF_UP);

            BigDecimal maintenanceFactor = BigDecimal.ONE.add(
                    tool.getMaintenancePercentage().divide(HUNDRED, INTERNAL_SCALE, RoundingMode.HALF_UP)
            );

            BigDecimal totalCostPerUse = costPerUse.multiply(maintenanceFactor);
            
            BigDecimal cost = pt.getUses().multiply(totalCostPerUse);
            total = total.add(cost);
        }
        return total;
    }

    private static BigDecimal calculateLaborCost(Project project) {
        return project.getLaborHours().multiply(project.getLaborCostPerHour())
                .setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateElectricityCost(Project project, LocalDateTime calculationDateTime, ElectricityPriceProvider priceProvider) {
        BigDecimal totalKWh = BigDecimal.ZERO.setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);

        for (ProjectMachine pm : project.getProjectMachines()) {
            var machine = pm.getMachine();
            
            BigDecimal powerKW = machine.getPowerWatts()
                    .divide(THOUSAND, INTERNAL_SCALE, RoundingMode.HALF_UP);
            
            BigDecimal energyKWh = powerKW.multiply(pm.getEstimatedHours());
            totalKWh = totalKWh.add(energyKWh);
        }

        BigDecimal price = priceProvider.getPricePerKWh(calculationDateTime);
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Electricity price cannot be negative");
        }

        return totalKWh.multiply(price).setScale(INTERNAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleToFinal(BigDecimal value) {
        return value.setScale(FINAL_SCALE, RoundingMode.HALF_UP);
    }
}
