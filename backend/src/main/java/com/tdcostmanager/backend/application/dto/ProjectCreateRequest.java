package com.tdcostmanager.backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record ProjectCreateRequest(
    @NotBlank String name,
    String description,
    @NotNull @PositiveOrZero BigDecimal laborHours,
    @NotNull @PositiveOrZero BigDecimal laborCostPerHour,
    @Valid List<ProjectMaterialRequest> materials,
    @Valid List<ProjectMachineRequest> machines,
    @Valid List<ProjectToolRequest> tools
) {}
