package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProjectToolRequest(
    @NotNull Long toolId,
    @NotNull @Positive BigDecimal uses
) {}
