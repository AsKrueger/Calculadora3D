package com.tdcostmanager.backend.application.dto;

import java.math.BigDecimal;

public record MachineResponse(
    Long id,
    String name,
    BigDecimal acquisitionCost,
    BigDecimal usefulLifeHours,
    BigDecimal powerWatts,
    BigDecimal maintenanceCostPerHour,
    boolean active
) {}
