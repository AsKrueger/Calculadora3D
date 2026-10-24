package com.tdcostmanager.backend.domain.model;

import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

@Entity
@Table(name = "quotes")
@EntityListeners(AuditingEntityListener.class)
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "margin_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal marginPercentage;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "safety_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal safetyPercentage;

    // Desglose por partida. Es null en los presupuestos anteriores a la V7, que no lo guardaban.
    @PositiveOrZero
    @Column(name = "material_cost", precision = 19, scale = 4)
    private BigDecimal materialCost;

    @PositiveOrZero
    @Column(name = "machine_cost", precision = 19, scale = 4)
    private BigDecimal machineCost;

    @PositiveOrZero
    @Column(name = "tool_cost", precision = 19, scale = 4)
    private BigDecimal toolCost;

    @PositiveOrZero
    @Column(name = "labor_cost", precision = 19, scale = 4)
    private BigDecimal laborCost;

    @PositiveOrZero
    @Column(name = "electricity_cost", precision = 19, scale = 4)
    private BigDecimal electricityCost;

    @PositiveOrZero
    @Column(name = "energy_kwh", precision = 19, scale = 4)
    private BigDecimal energyKWh;

    // Null también cuando el proyecto no consume energía: entonces no se consulta el precio
    @PositiveOrZero
    @Column(name = "electricity_price_per_kwh", precision = 19, scale = 8)
    private BigDecimal electricityPricePerKWh;

    @NotNull
    @PositiveOrZero
    @Column(name = "base_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseCost;

    @NotNull
    @PositiveOrZero
    @Column(name = "safety_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal safetyAmount;

    @NotNull
    @PositiveOrZero
    @Column(name = "adjusted_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal adjustedCost;

    @NotNull
    @PositiveOrZero
    @Column(name = "profit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal profitAmount;

    @NotNull
    @PositiveOrZero
    @Column(name = "final_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuoteStatus status;

    @NotNull
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;

    @NotNull
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Quote() {}

    /**
     * Crea un presupuesto en estado DRAFT a partir del resultado del cálculo.
     * Los importes quedan fijados: después solo puede cambiar el estado.
     */
    public static Quote create(
            Project project,
            BigDecimal marginPercentage,
            BigDecimal safetyPercentage,
            CostCalculationResult result,
            Instant calculatedAt) {

        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(marginPercentage, "Margin percentage cannot be null");
        Objects.requireNonNull(safetyPercentage, "Safety percentage cannot be null");
        Objects.requireNonNull(result, "Calculation result cannot be null");
        Objects.requireNonNull(calculatedAt, "Calculation instant cannot be null");

        if (marginPercentage.compareTo(BigDecimal.ZERO) < 0 || marginPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Margin percentage must be between 0 and 100");
        }
        if (safetyPercentage.compareTo(BigDecimal.ZERO) < 0 || safetyPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Safety percentage must be between 0 and 100");
        }

        boolean anyNegative = Stream.of(
                        result.materialCost(), result.machineCost(), result.toolCost(), result.laborCost(),
                        result.electricityCost(), result.baseCost(), result.safetyAmount(), result.adjustedCost(),
                        result.profitAmount(), result.finalPrice(), result.energyKWh())
                .map(value -> Objects.requireNonNull(value, "Calculation amounts cannot be null"))
                .anyMatch(value -> value.signum() < 0);
        if (anyNegative || (result.electricityPricePerKWh() != null && result.electricityPricePerKWh().signum() < 0)) {
            throw new IllegalArgumentException("Monetary costs cannot be negative");
        }

        Quote quote = new Quote();
        quote.project = project;
        quote.marginPercentage = marginPercentage;
        quote.safetyPercentage = safetyPercentage;
        quote.materialCost = result.materialCost();
        quote.machineCost = result.machineCost();
        quote.toolCost = result.toolCost();
        quote.laborCost = result.laborCost();
        quote.electricityCost = result.electricityCost();
        quote.energyKWh = result.energyKWh();
        quote.electricityPricePerKWh = result.electricityPricePerKWh();
        quote.baseCost = result.baseCost();
        quote.safetyAmount = result.safetyAmount();
        quote.adjustedCost = result.adjustedCost();
        quote.profitAmount = result.profitAmount();
        quote.finalPrice = result.finalPrice();
        quote.calculatedAt = calculatedAt;
        quote.status = QuoteStatus.DRAFT;
        return quote;
    }

    public void updateStatus(QuoteStatus status) {
        Objects.requireNonNull(status, "Quote status cannot be null");
        if (!this.status.canTransitionTo(status)) {
            throw new IllegalStateException(
                    "No se puede pasar un presupuesto de " + this.status + " a " + status);
        }
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }

    public BigDecimal getMarginPercentage() { return marginPercentage; }

    public BigDecimal getSafetyPercentage() { return safetyPercentage; }

    public BigDecimal getMaterialCost() { return materialCost; }

    public BigDecimal getMachineCost() { return machineCost; }

    public BigDecimal getToolCost() { return toolCost; }

    public BigDecimal getLaborCost() { return laborCost; }

    public BigDecimal getElectricityCost() { return electricityCost; }

    public BigDecimal getEnergyKWh() { return energyKWh; }

    public BigDecimal getElectricityPricePerKWh() { return electricityPricePerKWh; }

    public BigDecimal getBaseCost() { return baseCost; }

    public BigDecimal getSafetyAmount() { return safetyAmount; }

    public BigDecimal getAdjustedCost() { return adjustedCost; }

    public BigDecimal getProfitAmount() { return profitAmount; }

    public BigDecimal getFinalPrice() { return finalPrice; }

    public QuoteStatus getStatus() { return status; }

    public Instant getCalculatedAt() { return calculatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
