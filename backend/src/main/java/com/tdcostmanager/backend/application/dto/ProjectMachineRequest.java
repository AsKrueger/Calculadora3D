package com.tdcostmanager.backend.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProjectMachineRequest(
    @NotNull Long machineId,
    @NotNull @Positive BigDecimal estimatedHours
) {}
