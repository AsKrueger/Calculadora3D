package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.ProjectUpdateRequest;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldThrowExceptionWhenUpdatingArchivedProject() {
        Long projectId = 1L;
        Project archivedProject = new Project();
        archivedProject.setId(projectId);
        archivedProject.updateDetails("Archived", null);
        archivedProject.archive();

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Updated Name", "Desc", ProjectStatus.COMPLETED, BigDecimal.ONE, BigDecimal.TEN
        );

        Long ownerId = 7L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(projectRepository.findByIdAndOwnerId(projectId, ownerId)).thenReturn(Optional.of(archivedProject));

        assertThatThrownBy(() -> projectService.update(projectId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se puede modificar un proyecto archivado");
    }
}
