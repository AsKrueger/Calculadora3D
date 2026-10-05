package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.UnitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProjectMaterialRequest(
    @NotNull Long materialId,
    @NotNull @Positive BigDecimal quantityUsed,
    @NotNull UnitType unit
) {}
