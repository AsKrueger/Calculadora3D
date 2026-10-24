package com.tdcostmanager.app.ui.common

import com.tdcostmanager.app.domain.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class FormatTest {

    @Test
    fun moneyUsesSpanishSeparators() {
        assertEquals("41,84 €", Format.euros(41.842))
        assertEquals("1.234,50 €", Format.euros(1234.5))
        assertEquals("0,00 €", Format.euros(0.0))
    }

    @Test
    fun numbersDropUselessDecimals() {
        assertEquals("1.000", Format.number(1000.0))
        assertEquals("1,5", Format.number(1.5))
        assertEquals("0,0229", Format.number(0.0229, 4))
        assertEquals("145 g", Format.quantity(145.0, UnitType.G))
        assertEquals("1,5 h", Format.hours(1.5))
        assertEquals("30 %", Format.percent(30.0))
    }

    @Test
    fun instantsAreShownInTheGivenZoneAndLocalDatesAsIs() {
        assertEquals("24/10/2026 11:00", Format.dateTime("2026-10-24T09:00:00Z", ZoneId.of("Europe/Madrid")))
        assertEquals("24/10/2026 11:00", Format.dateTime("2026-10-24T11:00:00"))
        assertEquals("no es una fecha", Format.dateTime("no es una fecha"))
    }

    @Test
    fun parseDecimalAcceptsCommaAndDot() {
        assertEquals(1.5, Format.parseDecimal("1,5")!!, 0.0)
        assertEquals(1.5, Format.parseDecimal("1.5")!!, 0.0)
        assertEquals(1234.5, Format.parseDecimal("1.234,5")!!, 0.0)
        assertEquals(20.0, Format.parseDecimal(" 20 ")!!, 0.0)
    }

    @Test
    fun parseDecimalRejectsInvalidInput() {
        assertNull(Format.parseDecimal(""))
        assertNull(Format.parseDecimal("abc"))
        assertNull(Format.parseDecimal("1.2.3"))
    }

    @Test
    fun forInputRoundTripsThroughParseDecimal() {
        assertEquals("1000", Format.forInput(1000.0))
        assertEquals("1,5", Format.forInput(1.5))
        assertEquals(0.08, Format.parseDecimal(Format.forInput(0.08))!!, 0.0)
    }

    @Test
    fun validateNumberExplainsWhatIsWrong() {
        assertEquals("Obligatorio", validateNumber(""))
        assertNull(validateNumber("", required = false))
        assertEquals("Número no válido", validateNumber("1,2,3"))
        assertEquals("Debe ser mayor que 0", validateNumber("0", positive = true))
        assertEquals("Máximo 100", validateNumber("120", max = 100.0))
        assertNull(validateNumber("12,5"))
    }
}
