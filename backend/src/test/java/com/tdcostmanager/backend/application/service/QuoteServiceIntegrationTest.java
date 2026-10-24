package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.application.dto.QuoteResponse;
import com.tdcostmanager.backend.domain.model.Machine;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectMachine;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.model.QuoteStatus;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.MachineRepository;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import com.tdcostmanager.backend.infrastructure.electricity.EsiosElectricityProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WithMockUser(username = "owner@test.com")
class QuoteServiceIntegrationTest extends BaseIntegrationTest {

    private static final String ESIOS_RESPONSE = """
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

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private EsiosElectricityProvider esiosProvider;

    @Autowired
    private MockMvc mockMvc;

    private MockRestServiceServer esios;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@test.com");

        // Sustituye el RestClient real del proveedor por uno enlazado a un servidor simulado
        esios = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(esiosProvider, "restClient", restClientBuilder.build());
    }

    @Test
    void shouldGenerateAndPersistQuoteWithFullBreakdown() {
        esios.expect(requestTo(containsString("/indicators/1001")))
                .andRespond(withSuccess(ESIOS_RESPONSE, MediaType.APPLICATION_JSON));

        // Mano de obra 1 h × 10 €/h; máquina de 200 W durante 2 h (500 € / 5000 h + 0,05 €/h de mantenimiento)
        Project project = projectWithMachine();

        QuoteResponse response = quoteService.generateQuote(project.getId(), new QuoteCreateRequest(
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                LocalDateTime.of(2026, 9, 20, 12, 0)
        ));

        esios.verify();
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(response.materialCost()).isEqualByComparingTo("0");
        assertThat(response.machineCost()).isEqualByComparingTo("0.3000");
        assertThat(response.toolCost()).isEqualByComparingTo("0");
        assertThat(response.laborCost()).isEqualByComparingTo("10.0000");
        // 0,4 kWh a 100 €/MWh
        assertThat(response.energyKWh()).isEqualByComparingTo("0.4000");
        assertThat(response.electricityPricePerKWh()).isEqualByComparingTo("0.1");
        assertThat(response.electricityCost()).isEqualByComparingTo("0.0400");
        assertThat(response.baseCost()).isEqualByComparingTo("10.3400");
        assertThat(response.safetyAmount()).isEqualByComparingTo("0.5170");
        assertThat(response.adjustedCost()).isEqualByComparingTo("10.8570");
        assertThat(response.profitAmount()).isEqualByComparingTo("2.1714");
        assertThat(response.finalPrice()).isEqualByComparingTo("13.0284");
        // Las 12:00 del 20 de septiembre en Madrid (CEST, UTC+2)
        assertThat(response.calculatedAt()).isEqualTo(Instant.parse("2026-09-20T10:00:00Z"));

        // El desglose queda guardado, no solo en la respuesta
        QuoteResponse reloaded = quoteService.findById(project.getId(), response.id());
        assertThat(reloaded.machineCost()).isEqualByComparingTo("0.3000");
        assertThat(reloaded.electricityPricePerKWh()).isEqualByComparingTo("0.1");
    }

    @Test
    void shouldQuoteProjectWithoutMachinesWithoutCallingEsios() {
        Project project = new Project();
        project.setOwner(owner);
        project.updateDetails("Solo mano de obra", null);
        project.updateStatus(ProjectStatus.DRAFT);
        project.updateLabor(BigDecimal.ONE, BigDecimal.TEN);
        project = projectRepository.save(project);

        QuoteResponse response = quoteService.generateQuote(project.getId(), new QuoteCreateRequest(
                BigDecimal.valueOf(20), BigDecimal.valueOf(5), LocalDateTime.of(2026, 9, 20, 12, 0)));

        // Ninguna petición esperada: si se hubiese llamado a ESIOS, el servidor simulado habría fallado
        esios.verify();
        assertThat(response.electricityPricePerKWh()).isNull();
        assertThat(response.electricityCost()).isEqualByComparingTo("0");
        assertThat(response.finalPrice()).isEqualByComparingTo("12.6000");
    }

    @Test
    void shouldChangeStatusThroughValidTransitionsAndRejectInvalidOnes() throws Exception {
        esios.expect(requestTo(containsString("/indicators/1001")))
                .andRespond(withSuccess(ESIOS_RESPONSE, MediaType.APPLICATION_JSON));
        Project project = projectWithMachine();
        QuoteResponse quote = quoteService.generateQuote(project.getId(), new QuoteCreateRequest(
                BigDecimal.valueOf(20), BigDecimal.valueOf(5), LocalDateTime.of(2026, 9, 20, 12, 0)));
        String statusUrl = "/api/v1/projects/" + project.getId() + "/quotes/" + quote.id() + "/status";

        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.finalPrice").value(13.0284));

        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede pasar un presupuesto de SENT a DRAFT"));

        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/quotes/" + quote.id()))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    private Project projectWithMachine() {
        Machine machine = new Machine();
        machine.setOwner(owner);
        machine.setName("Prusa MK4");
        machine.setAcquisitionCost(new BigDecimal("500.00"));
        machine.setUsefulLifeHours(new BigDecimal("5000"));
        machine.setPowerWatts(new BigDecimal("200"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.05"));
        machine = machineRepository.save(machine);

        Project project = new Project();
        project.setOwner(owner);
        project.updateDetails("Integration Project", null);
        project.updateStatus(ProjectStatus.DRAFT);
        project.updateLabor(BigDecimal.ONE, BigDecimal.TEN);
        ProjectMachine pm = new ProjectMachine();
        pm.setMachine(machine);
        pm.setEstimatedHours(new BigDecimal("2"));
        project.addProjectMachine(pm);
        return projectRepository.save(project);
    }
}
