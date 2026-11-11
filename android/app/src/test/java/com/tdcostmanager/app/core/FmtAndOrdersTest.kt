package com.tdcostmanager.app.core

import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.core.calc.PieceMapper
import com.tdcostmanager.app.data.Filament
import com.tdcostmanager.app.data.db.MaterialAmount
import com.tdcostmanager.app.data.db.MaterialUse
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.data.db.OrderWithLines
import com.tdcostmanager.app.data.db.PieceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class FmtAndOrdersTest {

    private fun bd(s: String) = BigDecimal(s)

    @Test
    fun `acepta coma o punto decimal`() {
        assertEquals(bd("12.5"), Fmt.parse("12,5"))
        assertEquals(bd("12.5"), Fmt.parse("12.5"))
        assertEquals(bd("1234.5"), Fmt.parse("1.234,5"))
        assertEquals(bd("1234.5"), Fmt.parse("1,234.5"))
        assertNull(Fmt.parse(""))
        assertNull(Fmt.parse("abc"))
    }

    @Test
    fun `formato de importes segun idioma y moneda`() {
        val es = Fmt(Locale.forLanguageTag("es-ES"), "EUR")
        assertTrue(es.money(bd("1234.5")).replace(' ', ' ').contains("1234,50") ||
            es.money(bd("1234.5")).replace(' ', ' ').contains("1.234,50"))
        assertEquals("12,5", es.input(bd("12.50")))
        val us = Fmt(Locale.US, "USD")
        assertEquals("$1,234.50", us.money(bd("1234.5")))
        assertEquals("12.5", us.input(bd("12.500")))
    }

    @Test
    fun `gramos a partir de metros de filamento de 1,75 mm`() {
        // 1 m de PLA (1,24 g/cm³) ≈ 2,98 g
        val g = Filament.gramsFromMeters(bd("1"), bd("1.24"))
        assertTrue(g > bd("2.97") && g < bd("2.99"))
    }

    private fun line(qty: Int, price: String, cost: String, profit: String, vat: String = "21", mats: List<MaterialAmount> = emptyList()) =
        OrderLineEntity(orderId = 1, pieceId = null, description = "x", quantity = qty, unitPrice = bd(price),
            unitCost = bd(cost), unitProfit = bd(profit), vatPercent = bd(vat), materialsPerUnit = mats)

    @Test
    fun `totales con descuento y envio`() {
        val order = OrderEntity(id = 1, number = 1, discountPercent = bd("10"), shipping = bd("4.95"))
        val t = OrderMath.totals(order, listOf(line(2, "12.10", "5", "3"), line(1, "6.05", "2", "1.5")))
        assertEquals(bd("30.25"), t.subtotal)
        assertEquals(bd("3.03"), t.discount)
        assertEquals(bd("32.17"), t.total)
        assertEquals(bd("12.00"), t.cost)
        // 7,5 de beneficio menos el descuento sin IVA (3,03 / 1,21 = 2,5041)
        assertEquals(bd("5.00"), t.profit)
        assertEquals(3, t.units)
    }

    @Test
    fun `resumen del mes cuenta solo lo aceptado de ese mes`() {
        val zone = ZoneId.of("Europe/Madrid")
        fun at(day: LocalDate) = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val nov = LocalDate.of(2026, 11, 5)
        val orders = listOf(
            OrderWithLines(OrderEntity(id = 1, number = 1, status = OrderStatus.DELIVERED, createdAt = at(nov)), listOf(line(1, "10", "4", "4", "0"))),
            OrderWithLines(OrderEntity(id = 2, number = 2, status = OrderStatus.CANCELLED, createdAt = at(nov)), listOf(line(1, "99", "1", "1", "0"))),
            OrderWithLines(OrderEntity(id = 3, number = 3, status = OrderStatus.SENT, createdAt = at(nov)), listOf(line(1, "50", "1", "1", "0"))),
            OrderWithLines(OrderEntity(id = 4, number = 4, status = OrderStatus.ACCEPTED, createdAt = at(nov.minusMonths(1))), listOf(line(1, "70", "1", "1", "0")))
        )
        val s = OrderMath.summary(YearMonth.of(2026, 11), orders, zone)
        assertEquals(bd("10.00"), s.sales)
        assertEquals(bd("4.00"), s.profit)
        assertEquals(50, s.acceptanceRate)
        assertEquals(1, s.openOrders)
    }

    @Test
    fun `material por unidad reparte el desperdicio y las piezas por placa`() {
        val piece = PieceEntity(
            name = "p", printerId = null, printHours = BigDecimal.ONE, piecesPerPlate = 4,
            materials = listOf(MaterialUse(1, bd("30")), MaterialUse(2, bd("10"))), wasteAmount = bd("8"),
            laborRate = BigDecimal.ZERO, failurePercent = BigDecimal.ZERO, pricingMode = "MARKUP", pricingValue = BigDecimal.ZERO,
            channelId = null, vatPercent = BigDecimal.ZERO, unitCost = BigDecimal.ZERO, unitPrice = BigDecimal.ZERO, unitProfit = BigDecimal.ZERO
        )
        val perUnit = PieceMapper.materialsPerUnit(piece)
        // (30 + 6) / 4 y (10 + 2) / 4
        assertEquals(0, perUnit[0].amount.compareTo(bd("9")))
        assertEquals(0, perUnit[1].amount.compareTo(bd("3")))
        val consumption = OrderMath.materialConsumption(listOf(line(5, "1", "1", "1", mats = perUnit)))
        assertEquals(0, consumption.getValue(1).compareTo(bd("45")))
    }

    @Test
    fun `PVPC de la API de Red Electrica`() {
        val body = """
            {"data":{"type":"x"},"included":[
              {"type":"Precio mercado spot (€/MWh)","attributes":{"values":[{"value":90,"datetime":"2026-11-10T00:00:00.000+01:00"}]}},
              {"type":"PVPC","attributes":{"values":[
                {"value":180.0,"datetime":"2026-11-10T00:00:00.000+01:00"},
                {"value":100.0,"datetime":"2026-11-10T01:00:00.000+01:00"},
                {"value":110.0,"datetime":"2026-11-10T02:00:00.000+01:00"},
                {"value":120.0,"datetime":"2026-11-10T03:00:00.000+01:00"},
                {"value":250.0,"datetime":"2026-11-10T04:00:00.000+01:00"}
              ]}}
            ]}
        """.trimIndent()
        val day = PvpcClient.parse(body, LocalDate.of(2026, 11, 10))
        assertEquals(bd("0.1520"), day.average)
        assertEquals(1, day.cheapestWindowStart)
    }
}
