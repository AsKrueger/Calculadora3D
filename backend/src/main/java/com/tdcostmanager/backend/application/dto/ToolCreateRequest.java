package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ToolCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal acquisitionCost,
        @NotNull @Positive BigDecimal estimatedUses,
        @NotNull @Min(0) @Max(100) BigDecimal maintenancePercentage
) {}
