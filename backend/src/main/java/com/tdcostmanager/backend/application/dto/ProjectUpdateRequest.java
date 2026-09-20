package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProjectUpdateRequest(
    @NotBlank String name,
    String description,
    @NotNull ProjectStatus status,
    @NotNull @PositiveOrZero BigDecimal laborHours,
    @NotNull @PositiveOrZero BigDecimal laborCostPerHour
) {}
