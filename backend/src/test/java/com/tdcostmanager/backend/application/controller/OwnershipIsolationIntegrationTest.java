package com.tdcostmanager.backend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.MaterialCreateRequest;
import com.tdcostmanager.backend.application.dto.MaterialUpdateRequest;
import com.tdcostmanager.backend.application.dto.ProjectCreateRequest;
import com.tdcostmanager.backend.application.dto.ProjectMaterialRequest;
import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #42: cada usuario solo ve y modifica sus propios datos.
 * El usuario A crea un material y un proyecto; el usuario B no debe poder verlos ni usarlos.
 */
class OwnershipIsolationIntegrationTest extends BaseIntegrationTest {

    private static final String USER_A = "alice@test.com";
    private static final String USER_B = "bob@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long materialOfA;
    private Long projectOfA;

    @BeforeEach
    void userACreatesData() throws Exception {
        authenticateAs(USER_A);

        MaterialCreateRequest material = new MaterialCreateRequest(
                "PLA de A", null, new BigDecimal("20.00"), new BigDecimal("1000.00"),
                UnitType.G, MaterialCategory.FILAMENT);
        materialOfA = createAndGetId("/api/v1/materials", material);

        ProjectCreateRequest project = new ProjectCreateRequest(
                "Proyecto de A", null, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new ProjectMaterialRequest(materialOfA, new BigDecimal("50"), UnitType.G)),
                List.of(), List.of());
        projectOfA = createAndGetId("/api/v1/projects", project);
    }

    @Test
    void elPropietarioVeSusDatos() throws Exception {
        authenticateAs(USER_A);

        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/projects/" + projectOfA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Proyecto de A"));
    }

    @Test
    void otroUsuarioNoVeLosDatosEnLosListados() throws Exception {
        authenticateAs(USER_B);

        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void otroUsuarioRecibe404AlLeerEditarOBorrar() throws Exception {
        authenticateAs(USER_B);

        mockMvc.perform(get("/api/v1/materials/" + materialOfA))
                .andExpect(status().isNotFound());

        MaterialUpdateRequest update = new MaterialUpdateRequest(
                "Robado", null, BigDecimal.ONE, BigDecimal.ONE,
                UnitType.G, MaterialCategory.FILAMENT, true);
        mockMvc.perform(put("/api/v1/materials/" + materialOfA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/materials/" + materialOfA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/projects/" + projectOfA))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/projects/" + projectOfA))
                .andExpect(status().isNotFound());

        // Los datos de A siguen intactos
        authenticateAs(USER_A);
        mockMvc.perform(get("/api/v1/materials/" + materialOfA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PLA de A"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void otroUsuarioNoPuedeUsarMaterialesAjenosEnSusProyectos() throws Exception {
        authenticateAs(USER_B);

        ProjectCreateRequest project = new ProjectCreateRequest(
                "Proyecto de B", null, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new ProjectMaterialRequest(materialOfA, new BigDecimal("10"), UnitType.G)),
                List.of(), List.of());
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isNotFound());
    }

    @Test
    void otroUsuarioNoPuedePresupuestarNiVerPresupuestosDeProyectosAjenos() throws Exception {
        authenticateAs(USER_B);

        QuoteCreateRequest quote = new QuoteCreateRequest(
                BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.of(2026, 10, 23, 12, 0));
        mockMvc.perform(post("/api/v1/projects/" + projectOfA + "/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quote)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/projects/" + projectOfA + "/quotes"))
                .andExpect(status().isNotFound());
    }

    private Long createAndGetId(String url, Object body) throws Exception {
        String json = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
