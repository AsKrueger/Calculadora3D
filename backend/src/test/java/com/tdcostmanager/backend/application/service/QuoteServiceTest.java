package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private QuoteService quoteService;

    @Test
    void shouldThrowExceptionWhenGeneratingQuoteForArchivedProject() {
        Long projectId = 1L;
        Project archivedProject = new Project();
        archivedProject.setId(projectId);
        archivedProject.setStatus(ProjectStatus.ARCHIVED);

        QuoteCreateRequest request = new QuoteCreateRequest(
                BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now()
        );

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(archivedProject));

        assertThatThrownBy(() -> quoteService.generateQuote(projectId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pueden generar presupuestos para un proyecto archivado");
    }
}
