package com.tdcostmanager.backend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.MachineCreateRequest;
import com.tdcostmanager.backend.application.dto.MachineUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MachineControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldPerformFullCrudOnMachine() throws Exception {
        MachineCreateRequest createRequest = new MachineCreateRequest(
                "Prusa MK4",
                new BigDecimal("800.0000"),
                new BigDecimal("10000.0000"),
                new BigDecimal("250.0000"),
                new BigDecimal("0.1000")
        );

        // 1. Create
        String responseJson = mockMvc.perform(post("/api/v1/machines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Prusa MK4"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseJson).get("id").asLong();

        // 2. Get by ID
        mockMvc.perform(get("/api/v1/machines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acquisitionCost").value(800.0));

        // 3. Update
        MachineUpdateRequest updateRequest = new MachineUpdateRequest(
                "Prusa MK4 Upgraded",
                new BigDecimal("900.0000"),
                new BigDecimal("12000.0000"),
                new BigDecimal("300.0000"),
                new BigDecimal("0.1500"),
                true
        );

        mockMvc.perform(put("/api/v1/machines/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prusa MK4 Upgraded"));

        // 4. Deactivate
        mockMvc.perform(delete("/api/v1/machines/" + id))
                .andExpect(status().isNoContent());

        // 5. Verify Inactive
        mockMvc.perform(get("/api/v1/machines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
