package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.*;
import com.tdcostmanager.backend.domain.model.*;
import com.tdcostmanager.backend.domain.repository.MachineRepository;
import com.tdcostmanager.backend.domain.repository.MaterialRepository;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.domain.repository.ToolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Edición de las líneas de un proyecto y validaciones coherentes (Fase 5). */
@WithMockUser(username = "owner@test.com")
class ProjectLinesIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProjectService projectService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private MachineRepository machineRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private User owner;
    private Material pla;
    private Material petg;
    private Material resinInLitres;
    private Machine printer;
    private Tool sandpaper;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@test.com");
        pla = material("PLA", UnitType.G, null);
        petg = material("PETG", UnitType.KG, null);
        resinInLitres = material("Resina", UnitType.L, null);
        printer = new Machine();
        printer.setOwner(owner);
        printer.setName("Prusa");
        printer.setAcquisitionCost(new BigDecimal("800"));
        printer.setUsefulLifeHours(new BigDecimal("5000"));
        printer.setPowerWatts(new BigDecimal("120"));
        printer.setMaintenanceCostPerHour(new BigDecimal("0.05"));
        printer = machineRepository.save(printer);
        sandpaper = new Tool();
        sandpaper.setOwner(owner);
        sandpaper.setName("Lija");
        sandpaper.setAcquisitionCost(BigDecimal.TEN);
        sandpaper.setEstimatedUses(new BigDecimal("30"));
        sandpaper.setMaintenancePercentage(BigDecimal.ZERO);
        sandpaper = toolRepository.save(sandpaper);
    }

    @Test
    void updateReplacesOnlyTheListsThatAreSent() {
        ProjectResponse created = projectService.create(new ProjectCreateRequest(
                "Pieza", null, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new ProjectMaterialRequest(pla.getId(), new BigDecimal("100"), UnitType.G)),
                List.of(new ProjectMachineRequest(printer.getId(), new BigDecimal("2"))),
                List.of(new ProjectToolRequest(sandpaper.getId(), BigDecimal.ONE))));

        ProjectResponse updated = projectService.update(created.id(), new ProjectUpdateRequest(
                "Pieza v2", null, ProjectStatus.IN_PROGRESS, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new ProjectMaterialRequest(petg.getId(), new BigDecimal("150"), UnitType.G),
                        new ProjectMaterialRequest(pla.getId(), new BigDecimal("20"), UnitType.G)),
                List.of(),
                null));
        flushAndClear();

        assertThat(updated.materials()).extracting(ProjectResponse.MaterialItem::name).containsExactly("PETG", "PLA");
        assertThat(updated.machines()).isEmpty();
        // Las herramientas no se enviaron: se mantienen
        assertThat(updated.tools()).extracting(ProjectResponse.ToolItem::name).containsExactly("Lija");

        ProjectResponse reloaded = projectService.findById(created.id());
        assertThat(reloaded.materials()).hasSize(2);
        assertThat(reloaded.machines()).isEmpty();
        assertThat(reloaded.name()).isEqualTo("Pieza v2");
    }

    @Test
    void archivedProjectLinesCannotBeChanged() {
        ProjectResponse created = projectService.create(new ProjectCreateRequest("Viejo", null, BigDecimal.ONE, BigDecimal.ONE, null, null, null));
        projectService.delete(created.id());

        assertThatThrownBy(() -> projectService.update(created.id(), new ProjectUpdateRequest(
                "Viejo", null, ProjectStatus.ARCHIVED, BigDecimal.ONE, BigDecimal.ONE,
                List.of(new ProjectMaterialRequest(pla.getId(), BigDecimal.ONE, UnitType.G)), null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherUsersMaterialIsNotFoundWhenEditingLines() {
        User other = createUser("other@test.com");
        Material foreign = new Material();
        foreign.setOwner(other);
        foreign.setName("Ajeno");
        foreign.setPurchasePrice(BigDecimal.TEN);
        foreign.setQuantity(BigDecimal.TEN);
        foreign.setUnit(UnitType.G);
        foreign.setCategory(MaterialCategory.FILAMENT);
        Long foreignId = materialRepository.save(foreign).getId();
        ProjectResponse created = projectService.create(new ProjectCreateRequest("Mío", null, BigDecimal.ONE, BigDecimal.ONE, null, null, null));

        assertThatThrownBy(() -> projectService.update(created.id(), new ProjectUpdateRequest(
                "Mío", null, ProjectStatus.DRAFT, BigDecimal.ONE, BigDecimal.ONE,
                List.of(new ProjectMaterialRequest(foreignId, BigDecimal.ONE, UnitType.G)), null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void massCannotBeUsedForAMaterialBoughtByVolumeUnlessItHasDensity() {
        ProjectCreateRequest usingGrams = new ProjectCreateRequest("Figura", null, BigDecimal.ONE, BigDecimal.ONE,
                List.of(new ProjectMaterialRequest(resinInLitres.getId(), new BigDecimal("110"), UnitType.G)), null, null);

        assertThatThrownBy(() -> projectService.create(usingGrams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resina: Para convertir g a L el material necesita una densidad (g/ml)");

        resinInLitres.setDensityGPerMl(new BigDecimal("1.1"));
        materialRepository.save(resinInLitres);
        assertThat(projectService.create(usingGrams).materials()).hasSize(1);
    }

    // Cada inserción fallida aborta la transacción en PostgreSQL, así que va en un test propio

    @Test
    void databaseRejectsMaterialLinesWithZeroQuantity() {
        Long projectId = emptyProjectId();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO project_materials (project_id, material_id, quantity_used, unit) VALUES (?, ?, 0, 'G')",
                projectId, pla.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsToolLinesWithZeroUses() {
        Long projectId = emptyProjectId();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO project_tools (project_id, tool_id, uses) VALUES (?, ?, 0)",
                projectId, sandpaper.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long emptyProjectId() {
        ProjectResponse created = projectService.create(new ProjectCreateRequest("Pieza", null, BigDecimal.ONE, BigDecimal.ONE, null, null, null));
        flushAndClear();
        return created.id();
    }

    private Material material(String name, UnitType unit, BigDecimal density) {
        Material m = new Material();
        m.setOwner(owner);
        m.setName(name);
        m.setPurchasePrice(new BigDecimal("30"));
        m.setQuantity(unit == UnitType.G ? new BigDecimal("1000") : BigDecimal.ONE);
        m.setUnit(unit);
        m.setCategory(MaterialCategory.FILAMENT);
        m.setDensityGPerMl(density);
        return materialRepository.save(m);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
