package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.UnitType;
import java.math.BigDecimal;

public record MaterialResponse(
    Long id,
    String name,
    String description,
    BigDecimal purchasePrice,
    BigDecimal quantity,
    UnitType unit,
    MaterialCategory category,
    boolean active
) {}
