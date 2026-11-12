package com.tdcostmanager.app.core.calc

import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.MaterialAmount
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.data.db.OrderWithLines
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.data.db.PrinterEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class OrderTotals(
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val shipping: BigDecimal,
    val total: BigDecimal,
    val cost: BigDecimal,
    /** Beneficio estimado: el de cada línea menos el descuento sin IVA. */
    val profit: BigDecimal,
    val units: Int
)

object OrderMath {

    private val HUNDRED = BigDecimal(100)

    fun totals(order: OrderEntity, lines: List<OrderLineEntity>): OrderTotals {
        val subtotal = lines.fold(BigDecimal.ZERO) { a, l -> a + l.unitPrice * BigDecimal(l.quantity) }
        // El descuento va de 0 a 100 %: con más, el total y el beneficio saldrían negativos
        val discountPercent = order.discountPercent.max(BigDecimal.ZERO).min(HUNDRED)
        val discount = subtotal.multiply(discountPercent)
            .divide(HUNDRED, 2, RoundingMode.HALF_UP)
        // Un envío negativo no se suma al total, así que tampoco se muestra ni se exporta
        val shipping = order.shipping.max(BigDecimal.ZERO)
        val total = subtotal - discount + shipping
        val cost = lines.fold(BigDecimal.ZERO) { a, l -> a + l.unitCost * BigDecimal(l.quantity) }
        val lineProfit = lines.fold(BigDecimal.ZERO) { a, l -> a + l.unitProfit * BigDecimal(l.quantity) }
        val vat = lines.maxOfOrNull { it.vatPercent } ?: BigDecimal.ZERO
        val discountNet = discount.divide(BigDecimal.ONE + vat.divide(HUNDRED, 6, RoundingMode.HALF_UP), 4, RoundingMode.HALF_UP)
        return OrderTotals(
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP),
            discount = discount,
            shipping = shipping.setScale(2, RoundingMode.HALF_UP),
            total = total.setScale(2, RoundingMode.HALF_UP),
            cost = cost.setScale(2, RoundingMode.HALF_UP),
            profit = (lineProfit - discountNet).setScale(2, RoundingMode.HALF_UP),
            units = lines.sumOf { it.quantity }
        )
    }

    data class MonthSummary(
        val month: YearMonth,
        val sales: BigDecimal,
        val cost: BigDecimal,
        val profit: BigDecimal,
        val wonOrders: Int,
        val decidedOrders: Int,
        val openOrders: Int
    ) {
        /** Presupuestos aceptados sobre los que el cliente ya decidió. Null si no hay ninguno. */
        val acceptanceRate: Int?
            get() = if (decidedOrders == 0) null else (wonOrders * 100 + decidedOrders / 2) / decidedOrders
    }

    fun summary(month: YearMonth, orders: List<OrderWithLines>, zone: ZoneId = ZoneId.systemDefault()): MonthSummary {
        val inMonth = orders.filter { YearMonth.from(Instant.ofEpochMilli(it.order.createdAt).atZone(zone)) == month }
        val won = inMonth.filter { it.order.status.isWon }
        val totals = won.map { totals(it.order, it.lines) }
        return MonthSummary(
            month = month,
            sales = totals.fold(BigDecimal.ZERO) { a, t -> a + t.total },
            cost = totals.fold(BigDecimal.ZERO) { a, t -> a + t.cost },
            profit = totals.fold(BigDecimal.ZERO) { a, t -> a + t.profit },
            wonOrders = won.size,
            decidedOrders = inMonth.count { it.order.status.isWon || it.order.status == OrderStatus.CANCELLED },
            openOrders = inMonth.count { it.order.status == OrderStatus.DRAFT || it.order.status == OrderStatus.SENT }
        )
    }

    /** Material que consume el pedido entero, por material. */
    fun materialConsumption(lines: List<OrderLineEntity>): Map<Long, BigDecimal> =
        lines.flatMap { l -> l.materialsPerUnit.map { it.materialId to it.amount * BigDecimal(l.quantity) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, v) -> v.fold(BigDecimal.ZERO, BigDecimal::add) }
}

/** Convierte una pieza guardada en la entrada del motor, con el catálogo y los ajustes actuales. */
object PieceMapper {

    fun input(
        piece: PieceEntity,
        printers: List<PrinterEntity>,
        materials: List<MaterialEntity>,
        extras: List<ExtraEntity>,
        channels: List<ChannelEntity>,
        energyPrice: BigDecimal,
        rounding: PriceRounding
    ): CostInput {
        val printer = printers.firstOrNull { it.id == piece.printerId }
        val mats = piece.materials.mapNotNull { use ->
            materials.firstOrNull { it.id == use.materialId }?.let { MaterialLine(use.amount, it.unitPrice) }
        }
        val ex = piece.extras.mapNotNull { use ->
            extras.firstOrNull { it.id == use.extraId }?.let { ExtraLine(it.unitCost, use.quantity) }
        }
        val channel = channels.firstOrNull { it.id == piece.channelId }
        return CostInput(
            materials = mats,
            wasteAmount = piece.wasteAmount,
            printHours = piece.printHours,
            printer = printer?.let {
                PrinterSpec(it.averagePowerW, it.purchasePrice, it.lifetimeHours, it.maintenancePerHour)
            },
            energyPricePerKwh = energyPrice,
            piecesPerPlate = piece.piecesPerPlate.coerceAtLeast(1),
            prepMinutes = piece.prepMinutes,
            postMinutesPerPiece = piece.postMinutes,
            laborRatePerHour = piece.laborRate,
            extras = ex,
            failurePercent = piece.failurePercent,
            pricing = Pricing(
                runCatching { PricingMode.valueOf(piece.pricingMode) }.getOrDefault(PricingMode.MARKUP),
                piece.pricingValue
            ),
            channel = channel?.let { ChannelFees(it.feePercent, it.fixedFee) } ?: ChannelFees.NONE,
            vatPercent = piece.vatPercent,
            rounding = rounding
        )
    }

    /** Material por unidad (no por placa), para descontar stock al imprimir un pedido. */
    fun materialsPerUnit(piece: PieceEntity): List<MaterialAmount> {
        val pieces = BigDecimal(piece.piecesPerPlate.coerceAtLeast(1))
        val total = piece.materials.fold(BigDecimal.ZERO) { a, m -> a + m.amount }
        return piece.materials.map { m ->
            // El desperdicio se reparte entre los materiales en proporción a lo usado
            val waste = if (total.signum() > 0) piece.wasteAmount * m.amount.divide(total, 8, RoundingMode.HALF_UP) else BigDecimal.ZERO
            MaterialAmount(m.materialId, (m.amount + waste).divide(pieces, 4, RoundingMode.HALF_UP))
        }
    }
}
