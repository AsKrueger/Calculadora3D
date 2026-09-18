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

    public Quote() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public BigDecimal getMarginPercentage() { return marginPercentage; }
    public void setMarginPercentage(BigDecimal marginPercentage) { this.marginPercentage = marginPercentage; }

    public BigDecimal getSafetyPercentage() { return safetyPercentage; }
    public void setSafetyPercentage(BigDecimal safetyPercentage) { this.safetyPercentage = safetyPercentage; }

    public BigDecimal getBaseCost() { return baseCost; }
    public void setBaseCost(BigDecimal baseCost) { this.baseCost = baseCost; }

    public BigDecimal getAdjustedCost() { return adjustedCost; }
    public void setAdjustedCost(BigDecimal adjustedCost) { this.adjustedCost = adjustedCost; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
