package com.tdcostmanager.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteStatusTest {

    @Test
    fun transitionsMatchTheBackendLifecycle() {
        assertEquals(listOf(QuoteStatus.SENT, QuoteStatus.REJECTED), QuoteStatus.DRAFT.allowedTransitions)
        assertEquals(listOf(QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED), QuoteStatus.SENT.allowedTransitions)
    }

    @Test
    fun finalStatesHaveNoTransitions() {
        listOf(QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED).forEach {
            assertTrue(it.allowedTransitions.isEmpty())
        }
    }
}
