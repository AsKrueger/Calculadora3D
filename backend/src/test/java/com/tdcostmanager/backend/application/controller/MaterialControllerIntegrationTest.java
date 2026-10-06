package com.tdcostmanager.backend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.MaterialCreateRequest;
import com.tdcostmanager.backend.application.dto.MaterialUpdateRequest;
import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldPerformFullCrudOnMaterial() throws Exception {
        MaterialCreateRequest createRequest = new MaterialCreateRequest(
                "PLA Premium",
                "High quality filament",
                new BigDecimal("25.0000"),
                new BigDecimal("1.0000"),
                UnitType.KG,
                MaterialCategory.FILAMENT
        );

        // 1. Create
        String responseJson = mockMvc.perform(post("/api/v1/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PLA Premium"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseJson).get("id").asLong();

        // 2. Get by ID
        mockMvc.perform(get("/api/v1/materials/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("High quality filament"));

        // 3. Update
        MaterialUpdateRequest updateRequest = new MaterialUpdateRequest(
                "PLA Premium Updated",
                "Updated description",
                new BigDecimal("30.0000"),
                new BigDecimal("1.0000"),
                UnitType.KG,
                MaterialCategory.FILAMENT,
                true
        );

        mockMvc.perform(put("/api/v1/materials/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PLA Premium Updated"));

        // 4. Deactivate (Logical Delete)
        mockMvc.perform(delete("/api/v1/materials/" + id))
                .andExpect(status().isNoContent());

        // 5. Verify Inactive in GET
        mockMvc.perform(get("/api/v1/materials/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 6. List all
        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenMaterialNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/materials/999"))
                .andExpect(status().isNotFound());
    }
}
