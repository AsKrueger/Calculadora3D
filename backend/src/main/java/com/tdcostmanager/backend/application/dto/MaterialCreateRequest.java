package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record MaterialCreateRequest(
    @NotBlank String name,
    String description,
    @NotNull @PositiveOrZero BigDecimal purchasePrice,
    @NotNull @Positive BigDecimal quantity,
    @NotNull UnitType unit,
    @NotNull MaterialCategory category
) {}
