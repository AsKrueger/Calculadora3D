package com.tdcostmanager.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @PositiveOrZero
    @Column(name = "acquisition_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal acquisitionCost;

    @NotNull
    @Positive
    @Column(name = "useful_life_hours", nullable = false, precision = 19, scale = 4)
    private BigDecimal usefulLifeHours;

    @NotNull
    @PositiveOrZero
    @Column(name = "power_watts", nullable = false, precision = 19, scale = 4)
    private BigDecimal powerWatts;

    @NotNull
    @PositiveOrZero
    @Column(name = "maintenance_cost_per_hour", nullable = false, precision = 19, scale = 4)
    private BigDecimal maintenanceCostPerHour;

    @NotNull
    @Column(nullable = false)
    private boolean active = true;

    public Machine() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(BigDecimal acquisitionCost) { this.acquisitionCost = acquisitionCost; }

    public BigDecimal getUsefulLifeHours() { return usefulLifeHours; }
    public void setUsefulLifeHours(BigDecimal usefulLifeHours) { this.usefulLifeHours = usefulLifeHours; }

    public BigDecimal getPowerWatts() { return powerWatts; }
    public void setPowerWatts(BigDecimal powerWatts) { this.powerWatts = powerWatts; }

    public BigDecimal getMaintenanceCostPerHour() { return maintenanceCostPerHour; }
    public void setMaintenanceCostPerHour(BigDecimal maintenanceCostPerHour) { this.maintenanceCostPerHour = maintenanceCostPerHour; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
