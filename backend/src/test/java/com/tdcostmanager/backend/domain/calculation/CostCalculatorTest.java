package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostCalculatorTest {

    private final LocalDateTime testDateTime = LocalDateTime.of(2026, 9, 17, 12, 0);

    @Test
    void shouldPerformFullCalculationWithKnownValues() {
        // 1. Setup Material (PLA: 20€ / 1000g)
        Material material = new Material();
        material.setName("PLA");
        material.setPurchasePrice(new BigDecimal("20.0000"));
        material.setQuantity(new BigDecimal("1000.0000"));
        material.setUnit(UnitType.G);

        ProjectMaterial pm = new ProjectMaterial();
        pm.setMaterial(material);
        pm.setQuantityUsed(new BigDecimal("100.0000")); // 10% of spool -> 2.00€
        pm.setUnit(UnitType.G);

        // 2. Setup Machine (Ender 3: 200€ acq, 2000h life, 350W power, 0.05€/h maint)
        Machine machine = new Machine();
        machine.setName("Ender 3");
        machine.setAcquisitionCost(new BigDecimal("200.0000"));
        machine.setUsefulLifeHours(new BigDecimal("2000.0000")); // 0.10€/h amort
        machine.setPowerWatts(new BigDecimal("350.0000"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.0500")); // 0.15€/h total machine

        ProjectMachine pMach = new ProjectMachine();
        pMach.setMachine(machine);
        pMach.setEstimatedHours(new BigDecimal("10.0000")); // 1.50€ machine cost

        // 3. Setup Tool (Airbrush: 100€ acq, 100 uses, 10% maintenance)
        Tool tool = new Tool();
        tool.setName("Airbrush");
        tool.setAcquisitionCost(new BigDecimal("100.0000"));
        tool.setEstimatedUses(new BigDecimal("100.0000")); // 1.00€ per use
        tool.setMaintenancePercentage(new BigDecimal("10.00")); // 0.10€ per use maint -> 1.10€ total per use

        ProjectTool pt = new ProjectTool();
        pt.setTool(tool);
        pt.setUses(new BigDecimal("2.0000")); // 2.20€ tool cost

        // 4. Setup Project (1h labor at 15€/h)
        Project project = new Project();
        project.setLaborHours(new BigDecimal("1.0000"));
        project.setLaborCostPerHour(new BigDecimal("15.0000")); // 15.00€ labor
        project.addProjectMaterial(pm);
        project.addProjectMachine(pMach);
        project.addProjectTool(pt);

        // 5. Electricity (0.20€ / kWh)
        // Consumption: (350W / 1000) * 10h = 3.5 kWh
        // Cost: 3.5 * 0.20 = 0.70€
        ElectricityPriceProvider priceProvider = (dt) -> new BigDecimal("0.2000");

        // 6. Margins (5% safety, 20% profit)
        BigDecimal safety = new BigDecimal("5.00");
        BigDecimal margin = new BigDecimal("20.00");

        CostCalculationResult result = CostCalculator.calculate(project, testDateTime, margin, safety, priceProvider);

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
        Project project = new Project();
        ElectricityPriceProvider provider = (dt) -> BigDecimal.TEN;
        
        CostCalculationResult result = CostCalculator.calculate(project, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, provider);
        
        assertThat(result.finalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionForZeroUsefulLife() {
        Project project = new Project();
        Machine machine = new Machine();
        machine.setAcquisitionCost(BigDecimal.TEN);
        machine.setUsefulLifeHours(BigDecimal.ZERO);
        
        ProjectMachine pm = new ProjectMachine();
        pm.setMachine(machine);
        pm.setEstimatedHours(BigDecimal.ONE);
        project.addProjectMachine(pm);

        assertThatThrownBy(() -> CostCalculator.calculate(project, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, (dt) -> BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Machine useful life must be greater than zero");
    }

    @Test
    void shouldHandleNonExactDivisionsWithoutError() {
        Project project = new Project();
        
        Material material = new Material();
        material.setPurchasePrice(BigDecimal.TEN);
        material.setQuantity(new BigDecimal("3"));
        material.setUnit(UnitType.UNIT);
        
        ProjectMaterial pm = new ProjectMaterial();
        pm.setMaterial(material);
        pm.setQuantityUsed(BigDecimal.ONE);
        pm.setUnit(UnitType.UNIT);
        project.addProjectMaterial(pm);

        CostCalculationResult result = CostCalculator.calculate(project, testDateTime, BigDecimal.ZERO, BigDecimal.ZERO, (dt) -> BigDecimal.ONE);
        
        assertThat(result.finalPrice()).isEqualByComparingTo("3.3333");
    }

    @Test
    void shouldValidateNegativeInputs() {
        Project project = new Project();
        assertThatThrownBy(() -> CostCalculator.calculate(project, testDateTime, new BigDecimal("-1"), BigDecimal.ZERO, (dt) -> BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
