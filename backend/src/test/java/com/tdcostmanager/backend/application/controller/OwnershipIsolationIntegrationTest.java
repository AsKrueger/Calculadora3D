package com.tdcostmanager.backend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.*;
import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import com.tdcostmanager.backend.domain.model.*;
import com.tdcostmanager.backend.domain.repository.MaterialRepository;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.domain.repository.QuoteRepository;
import com.tdcostmanager.backend.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aislamiento entre usuarios (Issue #42): el usuario B no puede ver ni tocar los datos del usuario A.
 * Usa tokens JWT reales para recorrer la cadena completa de seguridad.
 */
class OwnershipIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    private long materialA;
    private long machineA;
    private long toolA;
    private long projectA;

    @BeforeEach
    void setUp() throws Exception {
        userA = createUser("alice@test.com");
        userB = createUser("bob@test.com");
        tokenA = jwtService.generateToken(userA);
        tokenB = jwtService.generateToken(userB);

        materialA = create(tokenA, "/api/v1/materials", new MaterialCreateRequest(
                "PLA de A", null, new BigDecimal("20.00"), new BigDecimal("1000"), UnitType.G, MaterialCategory.FILAMENT));
        machineA = create(tokenA, "/api/v1/machines", new MachineCreateRequest(
                "Impresora de A", new BigDecimal("500.00"), new BigDecimal("5000"), new BigDecimal("200"), new BigDecimal("0.05")));
        toolA = create(tokenA, "/api/v1/tools", new ToolCreateRequest(
                "Lija de A", null, new BigDecimal("10.00"), new BigDecimal("50"), new BigDecimal("10")));
        projectA = create(tokenA, "/api/v1/projects", new ProjectCreateRequest(
                "Proyecto de A", null, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new ProjectMaterialRequest(materialA, new BigDecimal("50"), UnitType.G)),
                List.of(new ProjectMachineRequest(machineA, BigDecimal.ONE)),
                List.of(new ProjectToolRequest(toolA, BigDecimal.ONE))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"materials", "machines", "tools", "projects"})
    void listsOnlyContainTheCurrentUsersData(String resource) throws Exception {
        mockMvc.perform(as(tokenB, get("/api/v1/" + resource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(as(tokenA, get("/api/v1/" + resource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idOf(resource)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"materials", "machines", "tools", "projects"})
    void anotherUsersResourceIsNotFoundOnRead(String resource) throws Exception {
        mockMvc.perform(as(tokenB, get("/api/v1/" + resource + "/" + idOf(resource))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"materials", "machines", "tools", "projects"})
    void anotherUsersResourceIsNotFoundOnUpdate(String resource) throws Exception {
        mockMvc.perform(as(tokenB, put("/api/v1/" + resource + "/" + idOf(resource)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBodyFor(resource))))
                .andExpect(status().isNotFound());

        // Los datos de A siguen intactos
        mockMvc.perform(as(tokenA, get("/api/v1/" + resource + "/" + idOf(resource))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(endsWith("de A")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"materials", "machines", "tools"})
    void anotherUsersCatalogItemIsNotFoundOnDelete(String resource) throws Exception {
        mockMvc.perform(as(tokenB, delete("/api/v1/" + resource + "/" + idOf(resource))))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenA, get("/api/v1/" + resource + "/" + idOf(resource))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void anotherUsersProjectIsNotFoundOnDelete() throws Exception {
        mockMvc.perform(as(tokenB, delete("/api/v1/projects/" + projectA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenA, get("/api/v1/projects/" + projectA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"materials", "machines", "tools"})
    void cannotCreateProjectWithAnotherUsersCatalogItem(String resource) throws Exception {
        long foreignId = idOf(resource);
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Proyecto de B", null, BigDecimal.ONE, BigDecimal.TEN,
                resource.equals("materials") ? List.of(new ProjectMaterialRequest(foreignId, BigDecimal.ONE, UnitType.G)) : List.of(),
                resource.equals("machines") ? List.of(new ProjectMachineRequest(foreignId, BigDecimal.ONE)) : List.of(),
                resource.equals("tools") ? List.of(new ProjectToolRequest(foreignId, BigDecimal.ONE)) : List.of());

        mockMvc.perform(as(tokenB, post("/api/v1/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenB, get("/api/v1/projects")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotQuoteOrReadQuotesOfAnotherUsersProject() throws Exception {
        Quote quoteA = saveQuoteForProjectA();

        QuoteCreateRequest quoteRequest = new QuoteCreateRequest(
                new BigDecimal("20"), new BigDecimal("5"), LocalDateTime.of(2026, 9, 20, 12, 0));

        // Se rechaza antes de consultar ESIOS: el proyecto no existe para B
        mockMvc.perform(as(tokenB, post("/api/v1/projects/" + projectA + "/quotes"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteRequest)))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenB, get("/api/v1/projects/" + projectA + "/quotes")))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenB, get("/api/v1/projects/" + projectA + "/quotes/" + quoteA.getId())))
                .andExpect(status().isNotFound());

        // Tampoco a través de un proyecto propio de B
        long projectB = create(tokenB, "/api/v1/projects", new ProjectCreateRequest(
                "Proyecto de B", null, BigDecimal.ONE, BigDecimal.TEN, List.of(), List.of(), List.of()));
        mockMvc.perform(as(tokenB, get("/api/v1/projects/" + projectB + "/quotes/" + quoteA.getId())))
                .andExpect(status().isNotFound());

        // A sigue viendo su presupuesto
        mockMvc.perform(as(tokenA, get("/api/v1/projects/" + projectA + "/quotes/" + quoteA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalPrice").value(12.6));
    }

    @Test
    void cannotChangeTheStatusOfAnotherUsersQuote() throws Exception {
        Quote quoteA = saveQuoteForProjectA();
        String body = objectMapper.writeValueAsString(new QuoteStatusUpdateRequest(QuoteStatus.SENT));

        mockMvc.perform(as(tokenB, patch("/api/v1/projects/" + projectA + "/quotes/" + quoteA.getId() + "/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(tokenA, get("/api/v1/projects/" + projectA + "/quotes/" + quoteA.getId())))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void ownerSentByTheClientIsIgnored() throws Exception {
        String body = """
                {
                  "name": "PLA de B",
                  "purchasePrice": 20.00,
                  "quantity": 1000,
                  "unit": "G",
                  "category": "FILAMENT",
                  "ownerId": %d,
                  "owner": { "id": %d }
                }
                """.formatted(userA.getId(), userA.getId());

        String response = mockMvc.perform(as(tokenB, post("/api/v1/materials"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();
        assertThat(materialRepository.findById(id).orElseThrow().getOwner().getId()).isEqualTo(userB.getId());
    }

    private Quote saveQuoteForProjectA() {
        CostCalculationResult result = new CostCalculationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.0000"), BigDecimal.ZERO,
                new BigDecimal("10.0000"), new BigDecimal("0.5000"), new BigDecimal("10.5000"),
                new BigDecimal("2.1000"), new BigDecimal("12.6000"), BigDecimal.ZERO, null);
        return quoteRepository.save(Quote.create(
                projectRepository.findById(projectA).orElseThrow(),
                new BigDecimal("20"), new BigDecimal("5"),
                result, Instant.parse("2026-09-20T10:00:00Z")));
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    private long create(String token, String url, Object body) throws Exception {
        String response = mockMvc.perform(as(token, post(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long idOf(String resource) {
        return switch (resource) {
            case "materials" -> materialA;
            case "machines" -> machineA;
            case "tools" -> toolA;
            case "projects" -> projectA;
            default -> throw new IllegalArgumentException(resource);
        };
    }

    private Object updateBodyFor(String resource) {
        return switch (resource) {
            case "materials" -> new MaterialUpdateRequest(
                    "Robado por B", null, BigDecimal.ONE, BigDecimal.ONE, UnitType.G, MaterialCategory.FILAMENT, true);
            case "machines" -> new MachineUpdateRequest(
                    "Robado por B", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true);
            case "tools" -> new ToolUpdateRequest(
                    "Robado por B", null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, true);
            case "projects" -> new ProjectUpdateRequest(
                    "Robado por B", null, ProjectStatus.DRAFT, BigDecimal.ONE, BigDecimal.ONE);
            default -> throw new IllegalArgumentException(resource);
        };
    }
}
