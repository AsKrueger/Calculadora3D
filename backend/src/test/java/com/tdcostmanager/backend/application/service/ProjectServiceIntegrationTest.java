package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.ProjectCreateRequest;
import com.tdcostmanager.backend.application.dto.ProjectMachineRequest;
import com.tdcostmanager.backend.application.dto.ProjectMaterialRequest;
import com.tdcostmanager.backend.domain.model.UnitType;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldRollbackProjectCreationWhenMachineIsNotFound() {
        // Arrange
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Atomic Fail Project",
                "This should not exist in DB",
                BigDecimal.ONE,
                BigDecimal.TEN,
                List.of(),
                List.of(new ProjectMachineRequest(999L, BigDecimal.ONE)), // Non-existent machine ID
                List.of()
        );

        // Act & Assert
        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(EntityNotFoundException.class);

        // Verify Rollback: The project must not be saved
        boolean projectExists = projectRepository.findAll().stream()
                .anyMatch(p -> p.getName().equals("Atomic Fail Project"));
        
        assertThat(projectExists).isFalse();
    }
}
