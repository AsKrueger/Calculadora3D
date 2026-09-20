package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteCreateRequest(
    @NotNull @Min(0) @Max(100) BigDecimal marginPercentage,
    @NotNull @Min(0) @Max(100) BigDecimal safetyPercentage,
    @NotNull LocalDateTime calculationDateTime
) {}
