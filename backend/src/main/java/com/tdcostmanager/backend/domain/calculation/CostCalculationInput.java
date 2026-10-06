package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.UnitType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record CostCalculationInput(
        BigDecimal laborHours,
        BigDecimal laborCostPerHour,
        List<MaterialInput> materials,
        List<MachineInput> machines,
        List<ToolInput> tools
) {
    public CostCalculationInput {
        Objects.requireNonNull(laborHours, "Labor hours cannot be null");
        Objects.requireNonNull(laborCostPerHour, "Labor cost per hour cannot be null");
        Objects.requireNonNull(materials, "Materials cannot be null");
        Objects.requireNonNull(machines, "Machines cannot be null");
        Objects.requireNonNull(tools, "Tools cannot be null");

        if (laborHours.compareTo(BigDecimal.ZERO) < 0 || laborCostPerHour.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Labor values cannot be negative");
        }
    }

    public record MaterialInput(
            BigDecimal quantityUsed,
            UnitType unit,
            BigDecimal purchasePrice,
            BigDecimal quantity,
            UnitType materialUnit,
            String name
    ) {
        public MaterialInput {
            Objects.requireNonNull(quantityUsed, "Quantity used cannot be null");
            Objects.requireNonNull(unit, "Unit cannot be null");
            Objects.requireNonNull(purchasePrice, "Purchase price cannot be null");
            Objects.requireNonNull(quantity, "Quantity cannot be null");
            Objects.requireNonNull(materialUnit, "Material unit cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
        }
    }

    public record MachineInput(
            BigDecimal estimatedHours,
            BigDecimal acquisitionCost,
            BigDecimal usefulLifeHours,
            BigDecimal powerWatts,
            BigDecimal maintenanceCostPerHour,
            String name
    ) {
        public MachineInput {
            Objects.requireNonNull(estimatedHours, "Estimated hours cannot be null");
            Objects.requireNonNull(acquisitionCost, "Acquisition cost cannot be null");
            Objects.requireNonNull(usefulLifeHours, "Useful life hours cannot be null");
            Objects.requireNonNull(powerWatts, "Power watts cannot be null");
            Objects.requireNonNull(maintenanceCostPerHour, "Maintenance cost per hour cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
        }
    }

    public record ToolInput(
            BigDecimal uses,
            BigDecimal acquisitionCost,
            BigDecimal estimatedUses,
            BigDecimal maintenancePercentage,
            String name
    ) {
        public ToolInput {
            Objects.requireNonNull(uses, "Uses cannot be null");
            Objects.requireNonNull(acquisitionCost, "Acquisition cost cannot be null");
            Objects.requireNonNull(estimatedUses, "Estimated uses cannot be null");
            Objects.requireNonNull(maintenancePercentage, "Maintenance percentage cannot be null");
            Objects.requireNonNull(name, "Name cannot be null");
        }
    }
}
