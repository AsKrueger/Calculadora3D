package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Machine;
import com.tdcostmanager.backend.domain.model.Material;
import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.Tool;
import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

public class CatalogRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Test
    void shouldPersistAndDeactivateMachine() {
        Machine machine = new Machine();
        machine.setName("Ender 3 V2");
        machine.setAcquisitionCost(new BigDecimal("250.0000"));
        machine.setUsefulLifeHours(new BigDecimal("2000.0000"));
        machine.setPowerWatts(new BigDecimal("350.0000"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.0500"));
        
        Machine saved = machineRepository.save(machine);
        assertThat(saved.getId()).isNotNull();
        
        saved.setActive(false);
        machineRepository.save(saved);
        
        Machine found = machineRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isActive()).isFalse();
    }

    @Test
    void shouldPersistMaterialWithPrecision() {
        Material material = new Material();
        material.setName("PLA White");
        material.setDescription("High quality PLA");
        material.setPurchasePrice(new BigDecimal("22.5000"));
        material.setQuantity(new BigDecimal("1000.0000"));
        material.setUnit(UnitType.G);
        material.setCategory(MaterialCategory.FILAMENT);
        
        Material saved = materialRepository.save(material);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPurchasePrice()).isEqualByComparingTo("22.5000");
    }

    @Test
    void shouldPersistToolWithPercentage() {
        Tool tool = new Tool();
        tool.setName("Airbrush");
        tool.setAcquisitionCost(new BigDecimal("85.0000"));
        tool.setEstimatedUses(new BigDecimal("100.0000"));
        tool.setMaintenancePercentage(new BigDecimal("15.50"));
        
        Tool saved = toolRepository.save(tool);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMaintenancePercentage()).isEqualByComparingTo("15.50");
    }
}
