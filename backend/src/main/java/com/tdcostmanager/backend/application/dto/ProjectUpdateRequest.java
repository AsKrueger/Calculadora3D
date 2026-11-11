package com.tdcostmanager.backend.application.dto;

import com.tdcostmanager.backend.domain.model.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/**
 * Edición de un proyecto. Cada lista de líneas que se envía sustituye a la actual;
 * si una lista es null, esas líneas no cambian.
 */
public record ProjectUpdateRequest(
    @NotBlank String name,
    String description,
    @NotNull ProjectStatus status,
    @NotNull @PositiveOrZero BigDecimal laborHours,
    @NotNull @PositiveOrZero BigDecimal laborCostPerHour,
    @Valid List<ProjectMaterialRequest> materials,
    @Valid List<ProjectMachineRequest> machines,
    @Valid List<ProjectToolRequest> tools
) {
    /** Edición solo de los datos generales, sin tocar las líneas. */
    public ProjectUpdateRequest(String name, String description, ProjectStatus status,
                                BigDecimal laborHours, BigDecimal laborCostPerHour) {
        this(name, description, status, laborHours, laborCostPerHour, null, null, null);
    }
}
