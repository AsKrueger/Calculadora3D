package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
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
        // Setup Catalog
        Machine machine = new Machine();
        machine.setName("Prusa i3");
        machine.setAcquisitionCost(new BigDecimal("800.00"));
        machine.setUsefulLifeHours(new BigDecimal("5000.00"));
        machine.setPowerWatts(new BigDecimal("200.00"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.10"));
        machineRepository.save(machine);

        Material material = new Material();
        material.setName("PETG Black");
        material.setPurchasePrice(new BigDecimal("25.00"));
        material.setQuantity(new BigDecimal("1000.00"));
        material.setUnit(UnitType.G);
        material.setCategory(MaterialCategory.FILAMENT);
        materialRepository.save(material);

        // Setup Project
        Project project = new Project();
        project.setName("Death Star Lamp");
        project.setStatus(ProjectStatus.DRAFT);
        Project savedProject = projectRepository.save(project);

        // Add Resource to Project
        ProjectMaterial pm = new ProjectMaterial();
        pm.setMaterial(material);
        pm.setQuantityUsed(new BigDecimal("450.5000"));
        pm.setUnit(UnitType.G);
        savedProject.addProjectMaterial(pm);
        projectMaterialRepository.save(pm);

        // Create Quote
        Quote quote = new Quote();
        quote.setProject(savedProject);
        quote.setMarginPercentage(new BigDecimal("20.00"));
        quote.setSafetyPercentage(new BigDecimal("5.00"));
        quote.setBaseCost(new BigDecimal("15.7500"));
        quote.setAdjustedCost(new BigDecimal("16.5375"));
        quote.setFinalPrice(new BigDecimal("19.8450"));
        quote.setStatus(QuoteStatus.DRAFT);
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
    }

    @Test
    void shouldMaintainIntegrityWhenDeactivatingMaterial() {
        Material material = new Material();
        material.setName("Resin Grey");
        material.setPurchasePrice(new BigDecimal("40.00"));
        material.setQuantity(new BigDecimal("1000.00"));
        material.setUnit(UnitType.ML);
        material.setCategory(MaterialCategory.RESIN);
        materialRepository.save(material);

        Project project = new Project();
        project.setName("Miniature Hero");
        project.setStatus(ProjectStatus.DRAFT);
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
