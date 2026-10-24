package com.tdcostmanager.backend.domain.model;

import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class QuoteTest {

    private static final Instant CALCULATED_AT = Instant.parse("2026-09-20T10:00:00Z");

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.updateDetails("Test Project", "Desc");
    }

    @Test
    void shouldCreateDraftQuoteWithFullBreakdown() {
        Quote quote = Quote.create(project, new BigDecimal("20.00"), new BigDecimal("5.00"), validResult(), CALCULATED_AT);

        assertEquals(project, quote.getProject());
        assertEquals(new BigDecimal("20.00"), quote.getMarginPercentage());
        assertEquals(new BigDecimal("5.00"), quote.getSafetyPercentage());
        assertEquals(new BigDecimal("2.0000"), quote.getMaterialCost());
        assertEquals(new BigDecimal("1.5000"), quote.getMachineCost());
        assertEquals(new BigDecimal("2.2000"), quote.getToolCost());
        assertEquals(new BigDecimal("15.0000"), quote.getLaborCost());
        assertEquals(new BigDecimal("0.7000"), quote.getElectricityCost());
        assertEquals(new BigDecimal("3.5000"), quote.getEnergyKWh());
        assertEquals(new BigDecimal("0.20000000"), quote.getElectricityPricePerKWh());
        assertEquals(new BigDecimal("21.4000"), quote.getBaseCost());
        assertEquals(new BigDecimal("1.0700"), quote.getSafetyAmount());
        assertEquals(new BigDecimal("22.4700"), quote.getAdjustedCost());
        assertEquals(new BigDecimal("4.4940"), quote.getProfitAmount());
        assertEquals(new BigDecimal("26.9640"), quote.getFinalPrice());
        assertEquals(CALCULATED_AT, quote.getCalculatedAt());
        assertEquals(QuoteStatus.DRAFT, quote.getStatus());
    }

    @Test
    void shouldRejectNegativeMonetaryValues() {
        CostCalculationResult negativeMaterial = new CostCalculationResult(
                new BigDecimal("-1.0000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        CostCalculationResult negativeFinalPrice = new CostCalculationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1.0000"), BigDecimal.ZERO, null);
        CostCalculationResult negativePrice = new CostCalculationResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("-0.1"));

        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, BigDecimal.TEN, BigDecimal.ONE, negativeMaterial, CALCULATED_AT));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, BigDecimal.TEN, BigDecimal.ONE, negativeFinalPrice, CALCULATED_AT));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, BigDecimal.TEN, BigDecimal.ONE, negativePrice, CALCULATED_AT));
    }

    @Test
    void shouldRejectInvalidPercentages() {
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, new BigDecimal("-1"), BigDecimal.ONE, validResult(), CALCULATED_AT));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, new BigDecimal("101"), BigDecimal.ONE, validResult(), CALCULATED_AT));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, BigDecimal.TEN, new BigDecimal("-1"), validResult(), CALCULATED_AT));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(project, BigDecimal.TEN, new BigDecimal("101"), validResult(), CALCULATED_AT));
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, SENT",
            "DRAFT, REJECTED",
            "SENT, ACCEPTED",
            "SENT, REJECTED",
            "SENT, EXPIRED"
    })
    void shouldAllowValidTransitions(QuoteStatus from, QuoteStatus to) {
        Quote quote = quoteIn(from);

        quote.updateStatus(to);

        assertEquals(to, quote.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, DRAFT",
            "DRAFT, ACCEPTED",
            "DRAFT, EXPIRED",
            "SENT, DRAFT",
            "SENT, SENT",
            "ACCEPTED, DRAFT",
            "ACCEPTED, REJECTED",
            "REJECTED, SENT",
            "EXPIRED, ACCEPTED"
    })
    void shouldRejectInvalidTransitions(QuoteStatus from, QuoteStatus to) {
        Quote quote = quoteIn(from);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> quote.updateStatus(to));
        assertEquals("No se puede pasar un presupuesto de " + from + " a " + to, ex.getMessage());
        assertEquals(from, quote.getStatus());
    }

    @Test
    void shouldKeepFinancialSnapshotWhenStatusChanges() {
        Quote quote = Quote.create(project, BigDecimal.TEN, BigDecimal.ONE, validResult(), CALCULATED_AT);

        quote.updateStatus(QuoteStatus.SENT);

        assertEquals(QuoteStatus.SENT, quote.getStatus());
        assertEquals(new BigDecimal("21.4000"), quote.getBaseCost());
        assertEquals(new BigDecimal("22.4700"), quote.getAdjustedCost());
        assertEquals(new BigDecimal("26.9640"), quote.getFinalPrice());
        assertEquals(CALCULATED_AT, quote.getCalculatedAt());
    }

    // Recorre el camino válido más corto hasta el estado pedido
    private Quote quoteIn(QuoteStatus status) {
        Quote quote = Quote.create(project, BigDecimal.TEN, BigDecimal.ONE, validResult(), CALCULATED_AT);
        switch (status) {
            case DRAFT -> { }
            case SENT -> quote.updateStatus(QuoteStatus.SENT);
            case REJECTED -> quote.updateStatus(QuoteStatus.REJECTED);
            case ACCEPTED, EXPIRED -> {
                quote.updateStatus(QuoteStatus.SENT);
                quote.updateStatus(status);
            }
        }
        return quote;
    }

    // Mismos valores que CostCalculatorTest.shouldPerformFullCalculationWithKnownValues
    private CostCalculationResult validResult() {
        return new CostCalculationResult(
                new BigDecimal("2.0000"),
                new BigDecimal("1.5000"),
                new BigDecimal("2.2000"),
                new BigDecimal("15.0000"),
                new BigDecimal("0.7000"),
                new BigDecimal("21.4000"),
                new BigDecimal("1.0700"),
                new BigDecimal("22.4700"),
                new BigDecimal("4.4940"),
                new BigDecimal("26.9640"),
                new BigDecimal("3.5000"),
                new BigDecimal("0.20000000")
        );
    }
}
