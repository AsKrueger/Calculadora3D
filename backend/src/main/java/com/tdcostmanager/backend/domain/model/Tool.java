package com.tdcostmanager.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Entity
@Table(name = "tools")
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @PositiveOrZero
    @Column(name = "acquisition_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal acquisitionCost;

    @NotNull
    @Positive
    @Column(name = "estimated_uses", nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedUses;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "maintenance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maintenancePercentage;

    @NotNull
    @Column(nullable = false)
    private boolean active = true;

    public Tool() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(BigDecimal acquisitionCost) { this.acquisitionCost = acquisitionCost; }

    public BigDecimal getEstimatedUses() { return estimatedUses; }
    public void setEstimatedUses(BigDecimal estimatedUses) { this.estimatedUses = estimatedUses; }

    public BigDecimal getMaintenancePercentage() { return maintenancePercentage; }
    public void setMaintenancePercentage(BigDecimal maintenancePercentage) { this.maintenancePercentage = maintenancePercentage; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
