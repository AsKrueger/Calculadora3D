package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(
    Long id,
    Long projectId,
    BigDecimal marginPercentage,
    BigDecimal safetyPercentage,
    BigDecimal baseCost,
    BigDecimal adjustedCost,
    BigDecimal finalPrice,
    QuoteStatus status,
    Instant createdAt
) {}
