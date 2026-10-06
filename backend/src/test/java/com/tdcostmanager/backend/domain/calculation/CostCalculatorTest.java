package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostCalculatorTest {

    private final LocalDateTime testDateTime = LocalDateTime.of(2026, 9, 17, 12, 0);

    @Test
    void shouldPerformFullCalculationWithKnownValues() {
        // 1. Setup Material (PLA: 20€ / 1000g)
        var materialInput = new CostCalculationInput.MaterialInput(
                new BigDecimal("100.0000"), // quantityUsed
                UnitType.G,
                new BigDecimal("20.0000"), // purchasePrice
                new BigDecimal("1000.0000"), // quantity
                UnitType.G,
                "PLA"
        );

        // 2. Setup Machine (Ender 3: 200€ acq, 2000h life, 350W power, 0.05€/h maint)
        var machineInput = new CostCalculationInput.MachineInput(
                new BigDecimal("10.0000"), // estimatedHours
                new BigDecimal("200.0000"), // acquisitionCost
                new BigDecimal("2000.0000"), // usefulLifeHours
                new BigDecimal("350.0000"), // powerWatts
                new BigDecimal("0.0500"), // maintenanceCostPerHour
                "Ender 3"
        );

        // 3. Setup Tool (Airbrush: 100€ acq, 100 uses, 10% maintenance)
        var toolInput = new CostCalculationInput.ToolInput(
                new BigDecimal("2.0000"), // uses
                new BigDecimal("100.0000"), // acquisitionCost
                new BigDecimal("100.0000"), // estimatedUses
                new BigDecimal("10.0000"), // maintenancePercentage
                "Airbrush"
        );

        // 4. Setup Input (1h labor at 15€/h)
        CostCalculationInput input = new CostCalculationInput(
                new BigDecimal("1.0000"),
                new BigDecimal("15.0000"),
                List.of(materialInput),
                List.of(machineInput),
                List.of(toolInput)
        );

        // 5. Electricity (0.20€ / kWh)
        ElectricityPriceProvider priceProvider = (dt) -> new BigDecimal("0.2000");

        // 6. Margins (5% safety, 20% profit)
        BigDecimal safety = new BigDecimal("5.00");
        BigDecimal margin = new BigDecimal("20.00");

        CostCalculationResult result = CostCalculator.calculate(input, testDateTime, margin, safety, priceProvider);

        assertThat(result.materialCost()).isEqualByComparingTo("2.0000");
        assertThat(result.machineCost()).isEqualByComparingTo("1.5000");
        assertThat(result.toolCost()).isEqualByComparingTo("2.2000");
        assertThat(result.laborCost()).isEqualByComparingTo("15.0000");
        assertThat(result.electricityCost()).isEqualByComparingTo("0.7000");
        assertThat(result.baseCost()).isEqualByComparingTo("21.4000");
        assertThat(result.safetyAmount()).isEqualByComparingTo("1.0700");
        assertThat(result.adjustedCost()).isEqualByComparingTo("22.4700");
        assertThat(result.profitAmount()).isEqualByComparingTo("4.4940");
        assertThat(result.finalPrice()).isEqualByComparingTo("26.9640");
    }

    @Test
    void shouldHandleEmptyProject() {
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()
        );
        ElectricityPriceProvider provider = (dt) -> BigDecimal.TEN;
        
        CostCalculationResult result = CostCalculator.calculate(input, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, provider);
        
        assertThat(result.finalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionForZeroUsefulLife() {
        var machineInput = new CostCalculationInput.MachineInput(
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.ZERO, "Machine"
        );
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(machineInput), List.of()
        );

        assertThatThrownBy(() -> CostCalculator.calculate(input, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, (dt) -> BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Machine useful life must be greater than zero");
    }

    @Test
    void shouldHandleNonExactDivisionsWithoutError() {
        var materialInput = new CostCalculationInput.MaterialInput(
                BigDecimal.ONE, UnitType.UNIT, BigDecimal.TEN, new BigDecimal("3"), UnitType.UNIT, "Mat"
        );
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(materialInput), List.of(), List.of()
        );

        CostCalculationResult result = CostCalculator.calculate(input, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, (dt) -> BigDecimal.ONE);
        
        assertThat(result.finalPrice()).isEqualByComparingTo("3.3333");
    }

    @Test
    void shouldHandleZeroMargins() {
        var materialInput = new CostCalculationInput.MaterialInput(
                BigDecimal.ONE, UnitType.UNIT, BigDecimal.TEN, BigDecimal.ONE, UnitType.UNIT, "Mat"
        );
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(materialInput), List.of(), List.of()
        );

        CostCalculationResult result = CostCalculator.calculate(input, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, (dt) -> BigDecimal.ONE);
        
        assertThat(result.baseCost()).isEqualByComparingTo("10.0000");
        assertThat(result.finalPrice()).isEqualByComparingTo("10.0000");
    }

    @Test
    void shouldHandleOneHundredPercentMargins() {
        var materialInput = new CostCalculationInput.MaterialInput(
                BigDecimal.ONE, UnitType.UNIT, BigDecimal.TEN, BigDecimal.ONE, UnitType.UNIT, "Mat"
        );
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(materialInput), List.of(), List.of()
        );

        BigDecimal hundred = new BigDecimal("100");
        CostCalculationResult result = CostCalculator.calculate(input, testDateTime, hundred, hundred, (dt) -> BigDecimal.ZERO);
        
        assertThat(result.baseCost()).isEqualByComparingTo("10.0000");
        assertThat(result.safetyAmount()).isEqualByComparingTo("10.0000");
        assertThat(result.adjustedCost()).isEqualByComparingTo("20.0000");
        assertThat(result.profitAmount()).isEqualByComparingTo("20.0000");
        assertThat(result.finalPrice()).isEqualByComparingTo("40.0000");
    }

    @Test
    void shouldValidateNegativeInputs() {
        CostCalculationInput input = new CostCalculationInput(
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), List.of()
        );
        assertThatThrownBy(() -> CostCalculator.calculate(input, testDateTime, new BigDecimal("-1"), BigDecimal.ZERO, (dt) -> BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
