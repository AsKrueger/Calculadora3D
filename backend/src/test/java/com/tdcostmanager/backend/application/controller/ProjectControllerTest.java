package com.tdcostmanager.backend.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdcostmanager.backend.BaseIntegrationTest;
import com.tdcostmanager.backend.application.dto.ProjectCreateRequest;
import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

class ProjectControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldReturn400WhenProjectNameIsBlank() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "", // Blank name
                "A project description",
                BigDecimal.ONE,
                BigDecimal.TEN,
                List.of(),
                List.of(),
                List.of()
        );

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("name")));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenLaborHoursAreNegative() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Invalid Project",
                "Desc",
                new BigDecimal("-5.0"), // Negative hours
                BigDecimal.TEN,
                List.of(),
                List.of(),
                List.of()
        );

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("laborHours")));
    }
}
