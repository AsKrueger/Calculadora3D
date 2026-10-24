package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import com.tdcostmanager.backend.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ProjectMaterialRepository projectMaterialRepository;

    @Test
    void shouldPersistProjectWithQuotesAndResources() {
        User owner = persistOwner();

        // Setup Catalog
        Machine machine = new Machine();
        machine.setOwner(owner);
        machine.setName("Prusa i3");
        machine.setAcquisitionCost(new BigDecimal("800.00"));
        machine.setUsefulLifeHours(new BigDecimal("5000.00"));
        machine.setPowerWatts(new BigDecimal("200.00"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.10"));
        machineRepository.save(machine);

        Material material = new Material();
        material.setOwner(owner);
        material.setName("PETG Black");
        material.setPurchasePrice(new BigDecimal("25.00"));
        material.setQuantity(new BigDecimal("1000.00"));
        material.setUnit(UnitType.G);
        material.setCategory(MaterialCategory.FILAMENT);
        materialRepository.save(material);

        // Setup Project
        Project project = new Project();
        project.setOwner(owner);
        project.updateDetails("Death Star Lamp", null);
        project.updateStatus(ProjectStatus.DRAFT);
        Project savedProject = projectRepository.save(project);

        // Add Resource to Project
        ProjectMaterial pm = new ProjectMaterial();
        pm.setMaterial(material);
        pm.setQuantityUsed(new BigDecimal("450.5000"));
        pm.setUnit(UnitType.G);
        savedProject.addProjectMaterial(pm);
        projectMaterialRepository.save(pm);

        // Create Quote
        CostCalculationResult result = new CostCalculationResult(
                new BigDecimal("15.7500"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("15.7500"), new BigDecimal("0.7875"), new BigDecimal("16.5375"),
                new BigDecimal("3.3075"), new BigDecimal("19.8450"), BigDecimal.ZERO, null);
        Quote quote = Quote.create(
                savedProject,
                new BigDecimal("20.00"),
                new BigDecimal("5.00"),
                result,
                Instant.parse("2026-09-20T10:00:00Z")
        );
        quoteRepository.save(quote);

        entityManager.flush();
        entityManager.clear();

        // Verify
        Project found = projectRepository.findById(savedProject.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getProjectMaterials()).hasSize(1);
        assertThat(found.getProjectMaterials().get(0).getQuantityUsed()).isEqualByComparingTo("450.5000");

        Quote foundQuote = quoteRepository.findAll().get(0);
        assertThat(foundQuote.getFinalPrice()).isEqualByComparingTo("19.8450");
        assertThat(foundQuote.getMaterialCost()).isEqualByComparingTo("15.7500");
        assertThat(foundQuote.getProfitAmount()).isEqualByComparingTo("3.3075");
        assertThat(foundQuote.getElectricityPricePerKWh()).isNull();
        assertThat(foundQuote.getCalculatedAt()).isEqualTo(Instant.parse("2026-09-20T10:00:00Z"));
    }

    @Test
    void shouldMaintainIntegrityWhenDeactivatingMaterial() {
        User owner = persistOwner();

        Material material = new Material();
        material.setOwner(owner);
        material.setName("Resin Grey");
        material.setPurchasePrice(new BigDecimal("40.00"));
        material.setQuantity(new BigDecimal("1000.00"));
        material.setUnit(UnitType.ML);
        material.setCategory(MaterialCategory.RESIN);
        materialRepository.save(material);

        Project project = new Project();
        project.setOwner(owner);
        project.updateDetails("Miniature Hero", null);
        project.updateStatus(ProjectStatus.DRAFT);
        projectRepository.save(project);

        ProjectMaterial pm = new ProjectMaterial();
        pm.setProject(project);
        pm.setMaterial(material);
        pm.setQuantityUsed(new BigDecimal("50.00"));
        pm.setUnit(UnitType.ML);
        projectMaterialRepository.save(pm);

        entityManager.flush();
        entityManager.clear();

        // Deactivate material
        Material materialToUpdate = materialRepository.findById(material.getId()).orElseThrow();
        materialToUpdate.setActive(false);
        materialRepository.save(materialToUpdate);

        entityManager.flush();
        entityManager.clear();

        // Verify ProjectMaterial still points to the same material
        ProjectMaterial foundPm = projectMaterialRepository.findAll().get(0);
        assertThat(foundPm.getMaterial().getId()).isEqualTo(material.getId());
        assertThat(foundPm.getMaterial().isActive()).isFalse();
    }
}
