package com.tdcostmanager.backend.application.dto;

import java.math.BigDecimal;

public record ToolResponse(
        Long id,
        String name,
        String description,
        BigDecimal acquisitionCost,
        BigDecimal estimatedUses,
        BigDecimal maintenancePercentage,
        boolean active
) {}
