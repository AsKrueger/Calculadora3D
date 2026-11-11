package com.tdcostmanager.app.core.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Motor de cálculo del coste y del precio de venta de una pieza impresa.
 *
 * Todo se calcula por placa (una impresión) y se reparte entre las piezas de la placa.
 * Lógica pura: sin Android, sin base de datos. BigDecimal con escala 8 en los pasos
 * intermedios y escala 4 en el resultado (HALF_UP), como el motor del backend original.
 */
object CostEngine {

    private const val SCALE = 8
    private const val OUT = 4
    private val HUNDRED = BigDecimal(100)
    private val SIXTY = BigDecimal(60)
    private val THOUSAND = BigDecimal(1000)

    /** Tasa de fallos máxima admitida: con un 100 % nunca saldría una pieza. */
    val MAX_FAILURE_PERCENT = BigDecimal(90)

    fun calculate(input: CostInput): CostResult {
        require(input.piecesPerPlate >= 1) { "piecesPerPlate must be >= 1" }
        val pieces = BigDecimal(input.piecesPerPlate)

        // Material de la placa: cada filamento por su precio unitario
        val materialPlate = input.materials.fold(zero()) { acc, m ->
            acc + m.amount.nonNeg() * m.unitPrice.nonNeg()
        }
        // Desperdicio (purga, torre, bordes) al precio medio ponderado de los materiales usados
        val totalAmount = input.materials.fold(zero()) { acc, m -> acc + m.amount.nonNeg() }
        val avgUnitPrice = if (totalAmount.signum() > 0) materialPlate.div(totalAmount) else
            input.materials.firstOrNull()?.unitPrice?.nonNeg() ?: zero()
        val wastePlate = input.wasteAmount.nonNeg() * avgUnitPrice

        val hours = input.printHours.nonNeg()
        val printer = input.printer
        val energyKwh = if (printer == null) zero() else printer.averagePowerW.nonNeg().div(THOUSAND) * hours
        val energyPlate = energyKwh * input.energyPricePerKwh.nonNeg()

        val wearPerHour = if (printer == null) zero() else {
            val amortization = if (printer.lifetimeHours.signum() > 0)
                printer.purchasePrice.nonNeg().div(printer.lifetimeHours) else zero()
            amortization + printer.maintenancePerHour.nonNeg()
        }
        val wearPlate = wearPerHour * hours

        val rate = input.laborRatePerHour.nonNeg()
        val prepPlate = input.prepMinutes.nonNeg().div(SIXTY) * rate

        // Por pieza
        val material = (materialPlate + wastePlate).div(pieces)
        val energy = energyPlate.div(pieces)
        val wear = wearPlate.div(pieces)
        val prep = prepPlate.div(pieces)
        val post = input.postMinutesPerPiece.nonNeg().div(SIXTY) * rate
        val labor = prep + post
        val extras = input.extras.fold(zero()) { acc, e -> acc + e.unitCost.nonNeg() * e.quantity.nonNeg() }

        // Fallos: una placa fallida se repite entera (material, luz, desgaste y preparación),
        // pero el posprocesado y los extras solo se gastan en las piezas buenas.
        val failure = input.failurePercent.nonNeg().min(MAX_FAILURE_PERCENT).div(HUNDRED)
        val reprintable = material + energy + wear + prep
        val failureCost = if (failure.signum() == 0) zero() else
            reprintable * failure.div(BigDecimal.ONE - failure)

        val cost = material + energy + wear + labor + extras + failureCost

        val pricing = price(cost, input.pricing, input.channel, input.vatPercent, input.rounding)

        return CostResult(
            material = material.out(),
            energy = energy.out(),
            wear = wear.out(),
            labor = labor.out(),
            extras = extras.out(),
            failure = failureCost.out(),
            totalCost = cost.out(),
            energyKwhPerPiece = energyKwh.div(pieces).out(),
            price = pricing.gross.out(),
            vat = pricing.vat.out(),
            channelFees = pricing.fees.out(),
            profit = pricing.profit.out(),
            feasible = pricing.feasible,
            piecesPerPlate = input.piecesPerPlate
        )
    }

    private data class Priced(
        val gross: BigDecimal,
        val vat: BigDecimal,
        val fees: BigDecimal,
        val profit: BigDecimal,
        val feasible: Boolean
    )

    /**
     * Precio con IVA (lo que paga el cliente) tal que, tras quitar el IVA y la comisión del canal,
     * queda el coste más el beneficio buscado:
     *   bruto / (1 + iva) - bruto * comisión - fijo = coste + beneficio
     * La comisión de los marketplaces se cobra sobre el total que paga el cliente.
     */
    private fun price(
        cost: BigDecimal,
        pricing: Pricing,
        channel: ChannelFees,
        vatPercent: BigDecimal,
        rounding: PriceRounding
    ): Priced {
        val vat = vatPercent.nonNeg().div(HUNDRED)
        val fee = channel.feePercent.nonNeg().div(HUNDRED)
        val fixed = channel.fixedFee.nonNeg()
        val netFactor = BigDecimal.ONE.div(BigDecimal.ONE + vat) - fee

        val gross: BigDecimal
        var feasible = true
        when (pricing.mode) {
            PricingMode.FIXED_PRICE -> gross = pricing.value.nonNeg()
            PricingMode.MARKUP, PricingMode.TARGET_PROFIT -> {
                val target = if (pricing.mode == PricingMode.MARKUP)
                    cost * pricing.value.nonNeg().div(HUNDRED) else pricing.value.nonNeg()
                if (netFactor.signum() <= 0) {
                    // Comisión e IVA se comen todo el precio: no hay precio que dé beneficio
                    feasible = false
                    gross = zero()
                } else {
                    gross = rounding.roundUp((cost + target + fixed).div(netFactor))
                }
            }
        }

        val vatAmount = gross - gross.div(BigDecimal.ONE + vat)
        val fees = if (gross.signum() == 0) zero() else gross * fee + fixed
        val profit = gross - vatAmount - fees - cost
        return Priced(gross, vatAmount, fees, profit, feasible)
    }

    private fun zero() = BigDecimal.ZERO.setScale(SCALE)
    private fun BigDecimal.nonNeg() = if (signum() < 0) BigDecimal.ZERO else this
    private fun BigDecimal.div(other: BigDecimal) = divide(other, SCALE, RoundingMode.HALF_UP)
    private operator fun BigDecimal.times(other: BigDecimal): BigDecimal =
        multiply(other, MathContext.DECIMAL128).setScale(SCALE, RoundingMode.HALF_UP)
    private fun BigDecimal.out() = setScale(OUT, RoundingMode.HALF_UP)
}

/** Redondeo hacia arriba del precio final, para precios «de tienda». */
enum class PriceRounding(val step: BigDecimal?) {
    NONE(null),
    CENTS_5(BigDecimal("0.05")),
    CENTS_10(BigDecimal("0.10")),
    CENTS_50(BigDecimal("0.50")),
    UNIT(BigDecimal.ONE);

    fun roundUp(value: BigDecimal): BigDecimal {
        val s = step ?: return value
        return value.divide(s, 0, RoundingMode.CEILING).multiply(s)
    }
}

enum class PricingMode {
    /** Beneficio como porcentaje sobre el coste. */
    MARKUP,
    /** Beneficio fijo por pieza. */
    TARGET_PROFIT,
    /** El usuario pone el precio y la app dice cuánto gana. */
    FIXED_PRICE
}

data class Pricing(val mode: PricingMode, val value: BigDecimal)

data class ChannelFees(val feePercent: BigDecimal, val fixedFee: BigDecimal) {
    companion object {
        val NONE = ChannelFees(BigDecimal.ZERO, BigDecimal.ZERO)
    }
}

data class PrinterSpec(
    val averagePowerW: BigDecimal,
    val purchasePrice: BigDecimal,
    val lifetimeHours: BigDecimal,
    val maintenancePerHour: BigDecimal
)

/** Un material usado en la placa: cantidad en su unidad (g o ml) y precio por esa unidad. */
data class MaterialLine(val amount: BigDecimal, val unitPrice: BigDecimal)

/** Un extra por pieza (imán, inserto, caja…): coste unitario y cuántos lleva cada pieza. */
data class ExtraLine(val unitCost: BigDecimal, val quantity: BigDecimal)

data class CostInput(
    val materials: List<MaterialLine>,
    val wasteAmount: BigDecimal = BigDecimal.ZERO,
    val printHours: BigDecimal,
    val printer: PrinterSpec?,
    val energyPricePerKwh: BigDecimal,
    val piecesPerPlate: Int = 1,
    val prepMinutes: BigDecimal = BigDecimal.ZERO,
    val postMinutesPerPiece: BigDecimal = BigDecimal.ZERO,
    val laborRatePerHour: BigDecimal = BigDecimal.ZERO,
    val extras: List<ExtraLine> = emptyList(),
    val failurePercent: BigDecimal = BigDecimal.ZERO,
    val pricing: Pricing,
    val channel: ChannelFees = ChannelFees.NONE,
    val vatPercent: BigDecimal = BigDecimal.ZERO,
    val rounding: PriceRounding = PriceRounding.NONE
)

/** Resultado por pieza. */
data class CostResult(
    val material: BigDecimal,
    val energy: BigDecimal,
    val wear: BigDecimal,
    val labor: BigDecimal,
    val extras: BigDecimal,
    val failure: BigDecimal,
    val totalCost: BigDecimal,
    val energyKwhPerPiece: BigDecimal,
    /** Precio de venta con IVA: lo que paga el cliente. */
    val price: BigDecimal,
    val vat: BigDecimal,
    val channelFees: BigDecimal,
    /** Lo que queda limpio tras coste, IVA y comisiones. Negativo = pérdida. */
    val profit: BigDecimal,
    /** Falso si la comisión y el IVA hacen imposible obtener beneficio. */
    val feasible: Boolean,
    val piecesPerPlate: Int
) {
    /** Partidas del coste en orden fijo, para el desglose y sus colores. */
    val parts: List<Pair<CostPart, BigDecimal>>
        get() = listOf(
            CostPart.MATERIAL to material,
            CostPart.ENERGY to energy,
            CostPart.WEAR to wear,
            CostPart.LABOR to labor,
            CostPart.EXTRAS to extras,
            CostPart.FAILURE to failure
        )

    /** Beneficio sobre el precio sin IVA, en %. Null si no hay precio. */
    val marginPercent: BigDecimal?
        get() {
            val net = price - vat
            if (net.signum() <= 0) return null
            return profit.multiply(BigDecimal(100)).divide(net, 1, RoundingMode.HALF_UP)
        }

    /** La partida que más pesa en el coste y su porcentaje. */
    val dominantPart: Pair<CostPart, Int>?
        get() {
            if (totalCost.signum() <= 0) return null
            val (part, value) = parts.maxBy { it.second }
            val pct = value.multiply(BigDecimal(100)).divide(totalCost, 0, RoundingMode.HALF_UP).toInt()
            return part to pct
        }
}

enum class CostPart { MATERIAL, ENERGY, WEAR, LABOR, EXTRAS, FAILURE }
