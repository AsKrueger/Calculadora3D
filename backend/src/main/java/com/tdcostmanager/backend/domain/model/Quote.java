package com.tdcostmanager.backend.domain.model;

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

    @NotNull
    @PositiveOrZero
    @Column(name = "base_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseCost;

    @NotNull
    @PositiveOrZero
    @Column(name = "adjusted_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal adjustedCost;

    @NotNull
    @PositiveOrZero
    @Column(name = "final_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuoteStatus status;

    @NotNull
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Quote() {}

    public static Quote create(
            Project project,
            BigDecimal marginPercentage,
            BigDecimal safetyPercentage,
            BigDecimal baseCost,
            BigDecimal adjustedCost,
            BigDecimal finalPrice,
            QuoteStatus status) {
        
        Objects.requireNonNull(project, "Project cannot be null");
        Objects.requireNonNull(marginPercentage, "Margin percentage cannot be null");
        Objects.requireNonNull(safetyPercentage, "Safety percentage cannot be null");
        Objects.requireNonNull(baseCost, "Base cost cannot be null");
        Objects.requireNonNull(adjustedCost, "Adjusted cost cannot be null");
        Objects.requireNonNull(finalPrice, "Final price cannot be null");
        Objects.requireNonNull(status, "Quote status cannot be null");

        if (marginPercentage.compareTo(BigDecimal.ZERO) < 0 || marginPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Margin percentage must be between 0 and 100");
        }
        if (safetyPercentage.compareTo(BigDecimal.ZERO) < 0 || safetyPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Safety percentage must be between 0 and 100");
        }
        if (baseCost.compareTo(BigDecimal.ZERO) < 0 || adjustedCost.compareTo(BigDecimal.ZERO) < 0 || finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Monetary costs cannot be negative");
        }

        Quote quote = new Quote();
        quote.project = project;
        quote.marginPercentage = marginPercentage;
        quote.safetyPercentage = safetyPercentage;
        quote.baseCost = baseCost;
        quote.adjustedCost = adjustedCost;
        quote.finalPrice = finalPrice;
        quote.status = status;
        return quote;
    }

    public void updateStatus(QuoteStatus status) {
        Objects.requireNonNull(status, "Quote status cannot be null");
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }

    public BigDecimal getMarginPercentage() { return marginPercentage; }

    public BigDecimal getSafetyPercentage() { return safetyPercentage; }

    public BigDecimal getBaseCost() { return baseCost; }

    public BigDecimal getAdjustedCost() { return adjustedCost; }

    public BigDecimal getFinalPrice() { return finalPrice; }

    public QuoteStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
