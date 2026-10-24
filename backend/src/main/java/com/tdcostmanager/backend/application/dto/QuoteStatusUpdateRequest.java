package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.QuoteStatus;
import jakarta.validation.constraints.NotNull;

public record QuoteStatusUpdateRequest(
    @NotNull QuoteStatus status
) {}
