package com.tdcostmanager.backend.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class QuoteTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.updateDetails("Test Project", "Desc");
    }

    @Test
    void shouldCreateQuoteSuccessfullyWithValidSnapshot() {
        Quote quote = Quote.create(
                project,
                new BigDecimal("20.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.0000"),
                new BigDecimal("10.5000"),
                new BigDecimal("12.6000"),
                QuoteStatus.DRAFT
        );

        assertNotNull(quote);
        assertEquals(project, quote.getProject());
        assertEquals(new BigDecimal("20.00"), quote.getMarginPercentage());
        assertEquals(new BigDecimal("5.00"), quote.getSafetyPercentage());
        assertEquals(new BigDecimal("10.0000"), quote.getBaseCost());
        assertEquals(new BigDecimal("10.5000"), quote.getAdjustedCost());
        assertEquals(new BigDecimal("12.6000"), quote.getFinalPrice());
        assertEquals(QuoteStatus.DRAFT, quote.getStatus());
    }

    @Test
    void shouldRejectNegativeMonetaryValues() {
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, BigDecimal.TEN, BigDecimal.ONE,
                new BigDecimal("-1.0000"), BigDecimal.TEN, BigDecimal.TEN, QuoteStatus.DRAFT
        ));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, new BigDecimal("-1.0000"), BigDecimal.TEN, QuoteStatus.DRAFT
        ));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("-1.0000"), QuoteStatus.DRAFT
        ));
    }

    @Test
    void shouldRejectInvalidPercentages() {
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, new BigDecimal("-1"), BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, QuoteStatus.DRAFT
        ));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, new BigDecimal("101"), BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, QuoteStatus.DRAFT
        ));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, BigDecimal.TEN, new BigDecimal("-1"),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, QuoteStatus.DRAFT
        ));
        assertThrows(IllegalArgumentException.class, () -> Quote.create(
                project, BigDecimal.TEN, new BigDecimal("101"),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, QuoteStatus.DRAFT
        ));
    }

    @Test
    void shouldAllowStatusUpdateWithoutAffectingFinancialSnapshot() {
        Quote quote = Quote.create(
                project,
                BigDecimal.TEN,
                BigDecimal.ONE,
                new BigDecimal("10.0000"),
                new BigDecimal("10.1000"),
                new BigDecimal("11.1100"),
                QuoteStatus.DRAFT
        );

        assertEquals(QuoteStatus.DRAFT, quote.getStatus());

        quote.updateStatus(QuoteStatus.SENT);
        assertEquals(QuoteStatus.SENT, quote.getStatus());

        // Verify financial snapshot remains unchanged (immutability test)
        assertEquals(new BigDecimal("10.0000"), quote.getBaseCost());
        assertEquals(new BigDecimal("10.1000"), quote.getAdjustedCost());
        assertEquals(new BigDecimal("11.1100"), quote.getFinalPrice());
        assertEquals(BigDecimal.TEN, quote.getMarginPercentage());
        assertEquals(BigDecimal.ONE, quote.getSafetyPercentage());
        assertEquals(project, quote.getProject());
    }
}
