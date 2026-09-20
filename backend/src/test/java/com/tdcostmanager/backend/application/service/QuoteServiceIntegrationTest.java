package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.application.dto.QuoteResponse;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class QuoteServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Test
    void shouldGenerateAndPersistQuoteIntegratingEsiosMock() {
        // 1. Arrange: Create a project
        Project project = new Project();
        project.setName("Integration Project");
        project.setStatus(ProjectStatus.DRAFT);
        project.setLaborHours(BigDecimal.ONE);
        project.setLaborCostPerHour(BigDecimal.TEN);
        Project savedProject = projectRepository.save(project);

        // 2. Mock ESIOS HTTP Response
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        
        String mockResponseJson = """
                {
                  "indicator": {
                    "values": [
                      {
                        "value": 100.00,
                        "datetime": "2026-09-20T12:00:00.000+02:00"
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("/indicators/1001")))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        // 3. Act
        QuoteCreateRequest request = new QuoteCreateRequest(
                BigDecimal.valueOf(20), 
                BigDecimal.valueOf(5), 
                LocalDateTime.of(2026, 9, 20, 12, 0)
        );

        QuoteResponse response = quoteService.generateQuote(savedProject.getId(), request);

        // 4. Assert
        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(savedProject.getId());
        // Base cost: Labor (1*10) + Electricity (0 if no machine) = 10.00
        // Safety 5% = 0.50 -> 10.50
        // Profit 20% of 10.50 = 2.10 -> 12.60 finalPrice
        assertThat(response.finalPrice()).isEqualByComparingTo("12.6000");
    }
}
