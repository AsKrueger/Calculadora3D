package com.tdcostmanager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class FmtTest {

    private fun bd(s: String) = BigDecimal(s)

    private val es = Fmt(Locale.forLanguageTag("es-ES"), "EUR")
    private val us = Fmt(Locale.US, "USD")

    @Test
    fun `parse quita cualquier espacio, tambien el fino del frances y el de no separacion`() {
        assertEquals(bd("1234.5"), Fmt.parse(" 1 234,5 "))
        assertEquals(bd("1234.5"), Fmt.parse("1 234,5"))
        assertEquals(bd("1234.5"), Fmt.parse("1 234,5"))
        assertEquals(bd("12.5"), Fmt.parse("\t12,5\n"))
        assertNull(Fmt.parse("   "))
    }

    @Test
    fun `parse lee lo que muestran los formatos de numero de cada idioma`() {
        for (tag in listOf("es-ES", "en-US", "pt-PT", "fr-FR", "de-DE", "it-IT")) {
            val shown = Fmt(Locale.forLanguageTag(tag), "EUR").number(bd("1234567.25"))
            assertEquals("$tag: «$shown»", bd("1234567.25"), Fmt.parse(shown))
        }
    }

    @Test
    fun `parse con varios separadores de miles y sin decimales`() {
        assertEquals(bd("1234567.5"), Fmt.parse("1.234.567,5"))
        assertEquals(bd("1234567.5"), Fmt.parse("1,234,567.5"))
        assertEquals(bd("-3.5"), Fmt.parse("-3,5"))
        assertNull(Fmt.parse("1,2,3"))
        assertNull(Fmt.parse("12 €"))
    }

    @Test
    fun `input sin ceros de sobra ni notacion cientifica`() {
        assertEquals("1000", es.input(bd("1E+3")))
        assertEquals("1000", us.input(bd("1000.00")))
        assertEquals("0,05", es.input(bd("0.050")))
        assertEquals("0", es.input(bd("0.000")))
        assertEquals("", es.input(null))
    }

    @Test
    fun `numero y porcentaje redondean hacia arriba y quitan ceros`() {
        assertEquals("2,5", es.number(bd("2.50")))
        assertEquals("1,24", es.number(bd("1.235")))
        assertEquals("1.2", us.number(bd("1.24"), maxDecimals = 1))
        assertEquals("1,234.57", us.number(bd("1234.567")))
        assertEquals("12,3 %", es.percent(bd("12.34")))
        assertEquals("—", es.number(null))
        assertEquals("—", es.percent(null))
    }

    @Test
    fun `importes nulos y de precision`() {
        assertEquals("—", es.money(null))
        assertEquals("$0.0123", us.moneyPrecise(bd("0.0123")))
        assertEquals("$0.50", us.moneyPrecise(bd("0.5")))
    }

    @Test
    fun `horas y minutos de impresion`() {
        assertEquals("0 min", es.hours(59))
        assertEquals("45 min", es.hours(45 * 60))
        assertEquals("1 h 0 min", es.hours(3600))
        assertEquals("2 h 5 min", es.hours(2 * 3600 + 5 * 60 + 59))
    }

    @Test
    fun `moneda desconocida vuelve a euros`() {
        assertEquals("EUR", Fmt(Locale.US, "XXX1").currency.currencyCode)
        assertEquals("USD", us.currency.currencyCode)
    }
}
