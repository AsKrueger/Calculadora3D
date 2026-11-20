package com.tdcostmanager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PvpcClientTest {

    private val day = LocalDate.of(2026, 11, 20)

    /** Respuesta mínima de REData con el bloque PVPC y, opcionalmente, otro indicador delante. */
    private fun body(values: List<Pair<String, String>>, withSpot: Boolean = true): String {
        val pvpcValues = values.joinToString(",") { (dt, v) -> """{"value":$v,"percentage":1,"datetime":"$dt"}""" }
        val spot = """{"type":"Precio mercado spot (€/MWh)","id":"600","attributes":{"values":[{"value":1,"datetime":"2026-11-20T00:00:00.000+01:00"}]}},"""
        return """
            {"data":{"type":"Precios mercado peninsular en tiempo real"},
             "included":[${if (withSpot) spot else ""}
               {"type":"PVPC","id":"1001","attributes":{"title":"PVPC","values":[$pvpcValues]}}]}
        """.trimIndent()
    }

    private fun hour(h: Int) = "2026-11-20T%02d:00:00.000+01:00".format(h)

    @Test
    fun `convierte de euros por MWh a euros por kWh y calcula la media`() {
        val result = PvpcClient.parse(body(listOf(hour(0) to "100", hour(1) to "150.5", hour(2) to "200")), day)

        assertEquals(day, result.date)
        assertEquals(listOf(0, 1, 2), result.hourly.map { it.first })
        assertEquals(BigDecimal("0.10000"), result.hourly[0].second)
        assertEquals(BigDecimal("0.15050"), result.hourly[1].second)
        assertEquals(BigDecimal("0.1502"), result.average)
    }

    @Test
    fun `ignora otros indicadores y reconoce el tipo PVPC sin distinguir mayusculas`() {
        val lower = body(listOf(hour(0) to "80")).replace("PVPC", "pvpc")
        // "pvpc" exacto en minúsculas también vale; el spot (valor 1) no debe colarse
        val result = PvpcClient.parse(lower, day)
        assertEquals(BigDecimal("0.08000"), result.hourly.single().second)
    }

    @Test
    fun `las horas se pasan a la zona de Madrid aunque vengan en UTC`() {
        val result = PvpcClient.parse(body(listOf("2026-11-20T09:00:00.000Z" to "120")), day)
        assertEquals(10, result.hourly.single().first)
    }

    @Test
    fun `una hora repetida por el cambio de hora solo cuenta una vez`() {
        val result = PvpcClient.parse(
            body(listOf(hour(2) to "100", "2026-11-20T02:00:00.000+01:00" to "300", hour(3) to "200")),
            day
        )
        assertEquals(listOf(2, 3), result.hourly.map { it.first })
        assertEquals(BigDecimal("0.1500"), result.average)
    }

    @Test
    fun `sin bloque PVPC o sin valores es un error`() {
        val noPvpc = """{"included":[{"type":"Precio mercado spot","attributes":{"values":[]}}]}"""
        assertThrows(IllegalStateException::class.java) { PvpcClient.parse(noPvpc, day) }
        assertThrows(IllegalArgumentException::class.java) { PvpcClient.parse(body(emptyList()), day) }
    }

    @Test
    fun `franja de tres horas mas barata`() {
        val prices = listOf(5, 4, 3, 9, 1, 1, 1, 8)
        val pvpc = PvpcDay(day, BigDecimal.ONE, prices.mapIndexed { h, p -> h to BigDecimal(p) })
        assertEquals(4, pvpc.cheapestWindowStart)
    }

    @Test
    fun `la franja solo admite horas seguidas`() {
        // 0,1 y 3,4 no forman tres horas seguidas; 20-22 sí aunque sean más caras
        val hours = listOf(0 to 1, 1 to 1, 3 to 1, 4 to 1, 20 to 9, 21 to 9, 22 to 9)
        val pvpc = PvpcDay(day, BigDecimal.ONE, hours.map { (h, p) -> h to BigDecimal(p) })
        assertEquals(20, pvpc.cheapestWindowStart)
    }

    @Test
    fun `la franja puede terminar a las 23 y no existe con menos de tres horas`() {
        val late = PvpcDay(day, BigDecimal.ONE, (0..23).map { h -> h to BigDecimal(if (h >= 21) 1 else 5) })
        assertEquals(21, late.cheapestWindowStart)

        val short = PvpcDay(day, BigDecimal.ONE, listOf(0 to BigDecimal.ONE, 1 to BigDecimal.ONE))
        assertNull(short.cheapestWindowStart)

        val gaps = PvpcDay(day, BigDecimal.ONE, listOf(0, 2, 4).map { it to BigDecimal.ONE })
        assertNull(gaps.cheapestWindowStart)
    }
}
