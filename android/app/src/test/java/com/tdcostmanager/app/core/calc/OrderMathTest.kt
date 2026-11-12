package com.tdcostmanager.app.core.calc

import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.data.db.OrderWithLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Casos límite de los totales y del resumen mensual de pedidos. */
class OrderMathTest {

    private fun bd(s: String) = BigDecimal(s)

    private fun line(qty: Int, price: String, cost: String = "0", profit: String = "0", vat: String = "21") =
        OrderLineEntity(orderId = 1, pieceId = null, description = "x", quantity = qty, unitPrice = bd(price),
            unitCost = bd(cost), unitProfit = bd(profit), vatPercent = bd(vat))

    @Test
    fun `un descuento de mas del 100 por cien se queda en el 100`() {
        val order = OrderEntity(id = 1, number = 1, discountPercent = bd("150"))
        val t = OrderMath.totals(order, listOf(line(2, "10", "4", "3")))
        assertEquals(bd("20.00"), t.subtotal)
        assertEquals(bd("20.00"), t.discount)
        assertEquals(bd("0.00"), t.total)
    }

    @Test
    fun `descuento y envio negativos no cuentan y el envio mostrado cuadra con el total`() {
        val order = OrderEntity(id = 1, number = 1, discountPercent = bd("-10"), shipping = bd("-5"))
        val t = OrderMath.totals(order, listOf(line(2, "10")))
        assertEquals(bd("0.00"), t.discount)
        assertEquals(bd("0.00"), t.shipping)
        assertEquals(bd("20.00"), t.total)
        assertEquals(0, (t.subtotal - t.discount + t.shipping).compareTo(t.total))
    }

    @Test
    fun `pedido sin lineas solo suma el envio`() {
        val order = OrderEntity(id = 1, number = 1, discountPercent = bd("10"), shipping = bd("4.95"))
        val t = OrderMath.totals(order, emptyList())
        assertEquals(bd("0.00"), t.subtotal)
        assertEquals(bd("4.95"), t.total)
        assertEquals(bd("0.00"), t.profit)
        assertEquals(0, t.units)
    }

    @Test
    fun `tasa de aceptacion nula sin pedidos decididos y redondeada al entero mas cercano`() {
        val zone = ZoneId.of("Europe/Madrid")
        val createdAt = LocalDate.of(2026, 11, 5).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        fun order(id: Long, status: OrderStatus) =
            OrderWithLines(OrderEntity(id = id, number = id.toInt(), status = status, createdAt = createdAt), listOf(line(1, "10", vat = "0")))
        val month = YearMonth.of(2026, 11)

        val onlyOpen = OrderMath.summary(month, listOf(order(1, OrderStatus.DRAFT), order(2, OrderStatus.SENT)), zone)
        assertNull(onlyOpen.acceptanceRate)
        assertEquals(2, onlyOpen.openOrders)
        assertEquals(0, onlyOpen.sales.signum())

        // 2 ganados de 3 decididos = 66,7 % → 67
        val decided = OrderMath.summary(month, listOf(
            order(1, OrderStatus.ACCEPTED), order(2, OrderStatus.PRINTED), order(3, OrderStatus.CANCELLED)
        ), zone)
        assertEquals(67, decided.acceptanceRate)
        assertEquals(2, decided.wonOrders)
        assertEquals(3, decided.decidedOrders)
        assertEquals(bd("20.00"), decided.sales)
    }
}
