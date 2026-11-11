package com.tdcostmanager.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CsvExportTest {

    @Test
    fun `en espanol separa con punto y coma y usa coma decimal`() {
        val f = CsvExport.Format(Locale.forLanguageTag("es-ES"))
        assertEquals(';', f.separator)
        assertEquals("12,50", f.num(BigDecimal("12.5")))
        assertEquals("a;\"b;c\";\"di \"\"x\"\"\"\r\n", f.row(listOf("a", f.cell("b;c"), f.cell("di \"x\""))))
    }

    @Test
    fun `en ingles separa con coma y usa punto decimal`() {
        val f = CsvExport.Format(Locale.US)
        assertEquals(',', f.separator)
        assertEquals("12.50", f.num(BigDecimal("12.5")))
        assertEquals("\"Smith, John\"", f.cell("Smith, John"))
        assertEquals("plain", f.cell("plain"))
    }
}
