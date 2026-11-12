package com.tdcostmanager.app.core.calc

import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.ExtraUse
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.MaterialUse
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.data.db.PrinterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** Paso de una pieza guardada a la entrada del motor de cálculo. */
class PieceMapperTest {

    private fun bd(s: String) = BigDecimal(s)

    private val printers = listOf(
        PrinterEntity(id = 1, name = "FDM", averagePowerW = bd("120"), purchasePrice = bd("800"),
            lifetimeHours = bd("8000"), maintenancePerHour = bd("0.05")),
        PrinterEntity(id = 2, name = "Resina", averagePowerW = bd("60"), purchasePrice = bd("400"),
            lifetimeHours = bd("3000"), maintenancePerHour = bd("0.10"))
    )
    private val materials = listOf(
        MaterialEntity(id = 10, name = "PLA", packagePrice = bd("20"), packageSize = bd("1000"), density = bd("1.24"))
    )
    private val extras = listOf(ExtraEntity(id = 5, name = "Imán", unitCost = bd("0.10")))
    private val channels = listOf(ChannelEntity(id = 3, name = "Tienda online", feePercent = bd("6.5"), fixedFee = bd("0.20")))

    private fun piece(
        printerId: Long? = 1,
        piecesPerPlate: Int = 1,
        pricingMode: String = "MARKUP",
        channelId: Long? = 3
    ) = PieceEntity(
        name = "Soporte", printerId = printerId, printHours = bd("2.5"), piecesPerPlate = piecesPerPlate,
        materials = listOf(MaterialUse(10, bd("50")), MaterialUse(99, bd("5"))), wasteAmount = bd("3"),
        prepMinutes = bd("10"), postMinutes = bd("5"), laborRate = bd("15"),
        extras = listOf(ExtraUse(5, bd("2")), ExtraUse(77, bd("1"))), failurePercent = bd("5"),
        pricingMode = pricingMode, pricingValue = bd("4"), channelId = channelId, vatPercent = bd("21"),
        unitCost = BigDecimal.ZERO, unitPrice = BigDecimal.ZERO, unitProfit = BigDecimal.ZERO
    )

    private fun map(p: PieceEntity) =
        PieceMapper.input(p, printers, materials, extras, channels, bd("0.18"), PriceRounding.CENTS_10)

    @Test
    fun `usa la impresora, materiales, extras y canal del catalogo`() {
        val input = map(piece(pricingMode = "TARGET_PROFIT"))

        assertEquals(PrinterSpec(bd("120"), bd("800"), bd("8000"), bd("0.05")), input.printer)
        assertEquals(listOf(MaterialLine(bd("50"), materials[0].unitPrice)), input.materials)
        assertEquals(listOf(ExtraLine(bd("0.10"), bd("2"))), input.extras)
        assertEquals(ChannelFees(bd("6.5"), bd("0.20")), input.channel)
        assertEquals(Pricing(PricingMode.TARGET_PROFIT, bd("4")), input.pricing)
        assertEquals(bd("0.18"), input.energyPricePerKwh)
        assertEquals(PriceRounding.CENTS_10, input.rounding)
        assertEquals(bd("2.5"), input.printHours)
        assertEquals(bd("3"), input.wasteAmount)
        assertEquals(bd("10"), input.prepMinutes)
        assertEquals(bd("5"), input.postMinutesPerPiece)
        assertEquals(bd("15"), input.laborRatePerHour)
        assertEquals(bd("5"), input.failurePercent)
        assertEquals(bd("21"), input.vatPercent)
    }

    @Test
    fun `lo que ya no esta en el catalogo se ignora en vez de fallar`() {
        val input = map(piece(printerId = 42, channelId = 9))
        // El material 99 y el extra 77 no existen: se descartan
        assertEquals(1, input.materials.size)
        assertEquals(1, input.extras.size)
        assertNull(input.printer)
        assertEquals(ChannelFees.NONE, input.channel)
        assertEquals(ChannelFees.NONE, map(piece(channelId = null)).channel)
    }

    @Test
    fun `modo de precio desconocido vuelve a margen sobre coste`() {
        assertEquals(Pricing(PricingMode.MARKUP, bd("4")), map(piece(pricingMode = "LEGACY")).pricing)
    }

    @Test
    fun `piezas por placa nunca baja de una y el motor puede calcular`() {
        val input = map(piece(piecesPerPlate = 0))
        assertEquals(1, input.piecesPerPlate)
        // Con 0 piezas por placa el motor lanzaría IllegalArgumentException
        assertEquals(1, CostEngine.calculate(input).piecesPerPlate)
    }
}
