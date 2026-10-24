package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Presupuesto con su desglose. En los presupuestos anteriores a la V7, las partidas
 * (materialCost ... electricityPricePerKWh) llegan como null porque no se guardaban.
 */
public record QuoteResponse(
    Long id,
    Long projectId,
    BigDecimal marginPercentage,
    BigDecimal safetyPercentage,
    BigDecimal materialCost,
    BigDecimal machineCost,
    BigDecimal toolCost,
    BigDecimal laborCost,
    BigDecimal electricityCost,
    BigDecimal energyKWh,
    BigDecimal electricityPricePerKWh,
    BigDecimal baseCost,
    BigDecimal safetyAmount,
    BigDecimal adjustedCost,
    BigDecimal profitAmount,
    BigDecimal finalPrice,
    QuoteStatus status,
    Instant calculatedAt,
    Instant createdAt
) {}
