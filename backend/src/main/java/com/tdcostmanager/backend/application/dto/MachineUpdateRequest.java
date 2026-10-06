package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record MachineUpdateRequest(
    @NotBlank String name,
    @NotNull @PositiveOrZero BigDecimal acquisitionCost,
    @NotNull @Positive BigDecimal usefulLifeHours,
    @NotNull @PositiveOrZero BigDecimal powerWatts,
    @NotNull @PositiveOrZero BigDecimal maintenanceCostPerHour,
    @NotNull Boolean active
) {}
