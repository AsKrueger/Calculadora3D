package com.tdcostmanager.backend.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.updateDetails("Test Project", "Description");
        project.updateLabor(BigDecimal.TEN, new BigDecimal("25.00"));
        project.updateStatus(ProjectStatus.DRAFT);
    }

    @Test
    void shouldUpdateDetailsSuccessfullyWhenActive() {
        project.updateDetails("Updated Name", "Updated Description");
        assertEquals("Updated Name", project.getName());
        assertEquals("Updated Description", project.getDescription());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> project.updateDetails("", "Desc"));
        assertThrows(IllegalArgumentException.class, () -> project.updateDetails(null, "Desc"));
    }

    @Test
    void shouldUpdateLaborSuccessfullyWhenActive() {
        project.updateLabor(BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, project.getLaborHours());
        assertEquals(BigDecimal.ZERO, project.getLaborCostPerHour());

        project.updateLabor(new BigDecimal("40.5"), new BigDecimal("15.75"));
        assertEquals(new BigDecimal("40.5"), project.getLaborHours());
        assertEquals(new BigDecimal("15.75"), project.getLaborCostPerHour());
    }

    @Test
    void shouldRejectNegativeLaborValues() {
        assertThrows(IllegalArgumentException.class, () -> project.updateLabor(new BigDecimal("-1"), BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> project.updateLabor(BigDecimal.TEN, new BigDecimal("-5")));
    }

    @Test
    void shouldArchiveAndPreventModifications() {
        assertFalse(project.isArchived());
        project.archive();
        assertTrue(project.isArchived());
        assertEquals(ProjectStatus.ARCHIVED, project.getStatus());

        assertThrows(IllegalStateException.class, () -> project.updateDetails("New Name", "New Desc"));
        assertThrows(IllegalStateException.class, () -> project.updateLabor(BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> project.updateStatus(ProjectStatus.IN_PROGRESS));
    }

    @Test
    void shouldPreventAddingResourcesWhenArchived() {
        project.archive();

        ProjectMaterial pm = new ProjectMaterial();
        assertThrows(IllegalStateException.class, () -> project.addProjectMaterial(pm));

        ProjectMachine pMachine = new ProjectMachine();
        assertThrows(IllegalStateException.class, () -> project.addProjectMachine(pMachine));

        ProjectTool pt = new ProjectTool();
        assertThrows(IllegalStateException.class, () -> project.addProjectTool(pt));
    }
}
