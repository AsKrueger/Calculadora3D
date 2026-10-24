package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.ProjectResponse;
import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.model.Quote;
import com.tdcostmanager.backend.domain.model.QuoteStatus;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.domain.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * El listado de proyectos incluye el último presupuesto de cada uno (Issue #51),
 * cargado con una sola consulta para toda la lista.
 */
@WithMockUser(username = "owner@test.com")
class ProjectLatestQuoteIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @SpyBean
    private QuoteRepository quoteRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@test.com");
    }

    @Test
    void listIncludesTheLatestQuoteOfEachProjectWithASingleQuery() {
        Project withQuotes = saveProject("Con presupuestos");
        saveProject("Sin presupuestos");
        Project another = saveProject("Otro");
        saveQuote(withQuotes, "10.0000");
        Quote latest = saveQuote(withQuotes, "25.5000");
        latest.updateStatus(QuoteStatus.SENT);
        Quote onlyQuote = saveQuote(another, "7.0000");
        clearInvocations(quoteRepository);

        Map<String, ProjectResponse> byName = projectService.findAll().stream()
                .collect(Collectors.toMap(ProjectResponse::name, p -> p));

        ProjectResponse.LatestQuote shown = byName.get("Con presupuestos").latestQuote();
        assertThat(shown.id()).isEqualTo(latest.getId());
        assertThat(shown.finalPrice()).isEqualByComparingTo("25.5000");
        assertThat(shown.status()).isEqualTo(QuoteStatus.SENT);
        assertThat(shown.calculatedAt()).isEqualTo(Instant.parse("2026-09-20T10:00:00Z"));
        assertThat(byName.get("Otro").latestQuote().id()).isEqualTo(onlyQuote.getId());
        assertThat(byName.get("Sin presupuestos").latestQuote()).isNull();

        // Una consulta para todos los proyectos, ninguna por proyecto
        verify(quoteRepository, times(1)).findLatestPerProjectByOwnerId(any());
        verify(quoteRepository, never()).findFirstByProjectIdOrderByIdDesc(any());
    }

    @Test
    void detailIncludesTheLatestQuoteAndNewProjectsHaveNone() {
        Project project = saveProject("Detalle");
        saveQuote(project, "10.0000");
        Quote latest = saveQuote(project, "12.0000");

        assertThat(projectService.findById(project.getId()).latestQuote().id()).isEqualTo(latest.getId());
        assertThat(projectService.findById(saveProject("Vacío").getId()).latestQuote()).isNull();
    }

    @Test
    void anotherUsersQuotesAreNotIncluded() {
        User other = createUser("other@test.com");
        Project foreign = new Project();
        foreign.setOwner(other);
        foreign.updateDetails("Ajeno", null);
        foreign.updateStatus(ProjectStatus.DRAFT);
        saveQuote(projectRepository.save(foreign), "99.0000");

        List<ProjectResponse> mine = projectService.findAll();

        assertThat(mine).isEmpty();
        assertThat(quoteRepository.findLatestPerProjectByOwnerId(owner.getId())).isEmpty();
    }

    private Project saveProject(String name) {
        Project project = new Project();
        project.setOwner(owner);
        project.updateDetails(name, null);
        project.updateStatus(ProjectStatus.DRAFT);
        return projectRepository.save(project);
    }

    private Quote saveQuote(Project project, String finalPrice) {
        BigDecimal price = new BigDecimal(finalPrice);
        CostCalculationResult result = new CostCalculationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, price, BigDecimal.ZERO,
                price, BigDecimal.ZERO, price, BigDecimal.ZERO, price, BigDecimal.ZERO, null);
        return quoteRepository.save(Quote.create(project, BigDecimal.ZERO, BigDecimal.ZERO, result, Instant.parse("2026-09-20T10:00:00Z")));
    }
}
