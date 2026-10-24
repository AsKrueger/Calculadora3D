package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.model.QuoteStatus;
import com.tdcostmanager.backend.domain.model.UnitType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    ProjectStatus status,
    BigDecimal laborHours,
    BigDecimal laborCostPerHour,
    List<MaterialItem> materials,
    List<MachineItem> machines,
    List<ToolItem> tools,
    Instant createdAt,
    Instant updatedAt,
    LatestQuote latestQuote
) {
    /** Resumen del presupuesto más reciente; null si el proyecto aún no tiene ninguno. */
    public record LatestQuote(Long id, BigDecimal finalPrice, QuoteStatus status, Instant calculatedAt) {}

    public record MaterialItem(Long materialId, String name, BigDecimal quantityUsed, UnitType unit) {}
    public record MachineItem(Long machineId, String name, BigDecimal estimatedHours) {}
    public record ToolItem(Long toolId, String name, BigDecimal uses) {}
}
