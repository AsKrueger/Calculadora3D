package com.tdcostmanager.app.core.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CostEngineTest {

    private fun bd(s: String) = BigDecimal(s)

    /** Caso de referencia calculado a mano. */
    private fun base(
        pricing: Pricing = Pricing(PricingMode.MARKUP, bd("40")),
        channel: ChannelFees = ChannelFees.NONE,
        vat: String = "0",
        pieces: Int = 1,
        rounding: PriceRounding = PriceRounding.NONE,
        failure: String = "10"
    ) = CostInput(
        materials = listOf(MaterialLine(bd("100"), bd("0.02"))),           // 2,00
        printHours = bd("2"),
        printer = PrinterSpec(bd("120"), bd("300"), bd("5000"), bd("0.05")), // 0,24 kWh; desgaste 0,11 €/h
        energyPricePerKwh = bd("0.20"),                                      // 0,048
        piecesPerPlate = pieces,
        prepMinutes = bd("10"),                                              // 2,50
        postMinutesPerPiece = bd("5"),                                       // 1,25
        laborRatePerHour = bd("15"),
        extras = listOf(ExtraLine(bd("0.05"), bd("2"))),                     // 0,10
        failurePercent = bd(failure),
        pricing = pricing,
        channel = channel,
        vatPercent = bd(vat),
        rounding = rounding
    )

    private fun assertMoney(expected: String, actual: BigDecimal, tolerance: String = "0.0001") {
        assertTrue("expected $expected but was $actual", (actual - bd(expected)).abs() <= bd(tolerance))
    }

    @Test
    fun `desglose de una pieza con todas las partidas`() {
        val r = CostEngine.calculate(base())
        assertMoney("2.0000", r.material)
        assertMoney("0.0480", r.energy)
        assertMoney("0.2200", r.wear)
        assertMoney("3.7500", r.labor)
        assertMoney("0.1000", r.extras)
        // Fallos: (2 + 0,048 + 0,22 + 2,5) · 0,1 / 0,9 — el acabado y los extras no se repiten
        assertMoney("0.5298", r.failure)
        assertMoney("6.6478", r.totalCost)
        assertMoney("0.2400", r.energyKwhPerPiece)
    }

    @Test
    fun `margen sobre el coste sin comisiones ni IVA`() {
        val r = CostEngine.calculate(base())
        assertMoney("9.3069", r.price)
        assertMoney("2.6591", r.profit)
        assertTrue(r.feasible)
    }

    @Test
    fun `con comision e IVA el beneficio limpio sigue siendo el buscado`() {
        val r = CostEngine.calculate(base(channel = ChannelFees(bd("9.5"), bd("0.45")), vat = "21"))
        // Lo que queda tras IVA y comisión es el mismo 40 % sobre el coste
        assertMoney("2.6591", r.profit, "0.0003")
        val net = r.price - r.vat
        assertMoney(r.channelFees.toPlainString(), r.price * bd("0.095") + bd("0.45"), "0.0002")
        assertMoney((net - r.channelFees - r.totalCost).toPlainString(), r.profit, "0.0002")
        assertTrue(r.price > bd("13"))
    }

    @Test
    fun `beneficio objetivo fijo por pieza`() {
        val r = CostEngine.calculate(base(pricing = Pricing(PricingMode.TARGET_PROFIT, bd("5")), vat = "21"))
        assertMoney("5.0000", r.profit, "0.0002")
    }

    @Test
    fun `precio fijo dice cuanto se gana o se pierde`() {
        val r = CostEngine.calculate(base(pricing = Pricing(PricingMode.FIXED_PRICE, bd("6.05")), vat = "21"))
        // 6,05 con IVA = 5,00 sin IVA; coste 6,6478 → pérdida
        assertMoney("6.0500", r.price)
        assertMoney("1.0500", r.vat)
        assertMoney("-1.6478", r.profit)
    }

    @Test
    fun `una comision imposible no da precio`() {
        val r = CostEngine.calculate(base(channel = ChannelFees(bd("90"), BigDecimal.ZERO), vat = "21"))
        assertFalse(r.feasible)
    }

    @Test
    fun `varias piezas por placa reparten lo comun`() {
        val one = CostEngine.calculate(base(failure = "0"))
        val four = CostEngine.calculate(base(pieces = 4, failure = "0"))
        assertMoney("0.5000", four.material)
        assertMoney("0.0550", four.wear)
        // Preparación por placa (2,50 / 4) + acabado por pieza (1,25)
        assertMoney("1.8750", four.labor)
        // Los extras son por pieza: no se reparten
        assertMoney(one.extras.toPlainString(), four.extras)
    }

    @Test
    fun `redondeo hacia arriba a 50 centimos`() {
        val r = CostEngine.calculate(base(rounding = PriceRounding.CENTS_50))
        assertMoney("9.5000", r.price)
        assertMoney("2.8522", r.profit)
    }

    @Test
    fun `sin impresora no hay luz ni desgaste`() {
        val r = CostEngine.calculate(base(failure = "0").copy(printer = null))
        assertEquals(0, r.energy.signum())
        assertEquals(0, r.wear.signum())
    }

    @Test
    fun `el desperdicio se paga al precio medio de los materiales`() {
        val input = base(failure = "0").copy(
            materials = listOf(MaterialLine(bd("50"), bd("0.02")), MaterialLine(bd("50"), bd("0.04"))),
            wasteAmount = bd("10")
        )
        // 1 + 2 + 10 g · 0,03
        assertMoney("3.3000", CostEngine.calculate(input).material)
    }

    @Test
    fun `la tasa de fallos se limita al 90 por ciento`() {
        val r = CostEngine.calculate(base(failure = "100"))
        assertTrue(r.failure.signum() > 0)
    }

    @Test
    fun `explica la partida dominante`() {
        val r = CostEngine.calculate(base(failure = "0"))
        val (part, pct) = r.dominantPart!!
        assertEquals(CostPart.LABOR, part)
        assertEquals(61, pct)
    }
}
