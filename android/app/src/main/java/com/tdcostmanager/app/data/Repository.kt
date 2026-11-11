package com.tdcostmanager.app.data

import androidx.room.withTransaction
import com.tdcostmanager.app.core.ImageStore
import com.tdcostmanager.app.core.calc.CostEngine
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.core.calc.PieceMapper
import com.tdcostmanager.app.data.db.AppDatabase
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.OrderStatus
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.data.settings.SettingsRepository
import java.math.BigDecimal

/** Operaciones que tocan varias tablas a la vez. */
class Repository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val images: ImageStore
) {

    /** Recalcula la pieza con el catálogo actual y la guarda. Devuelve su id. */
    suspend fun savePiece(piece: PieceEntity): Long {
        val calculated = recalculate(piece)
        val createdAt = if (piece.id != 0L) db.pieces().byId(piece.id)?.createdAt ?: piece.createdAt else piece.createdAt
        val id = db.pieces().upsert(calculated.copy(createdAt = createdAt, updatedAt = System.currentTimeMillis()))
        // Upsert devuelve -1 cuando actualiza una fila existente
        return if (id == -1L) piece.id else id
    }

    suspend fun recalculate(piece: PieceEntity): PieceEntity {
        val s = settings.current()
        val input = PieceMapper.input(
            piece, db.printers().all(), db.materials().all(), db.extras().all(), db.channels().all(),
            s.energyPrice, s.rounding
        )
        val r = CostEngine.calculate(input)
        return piece.copy(unitCost = r.totalCost, unitPrice = r.price, unitProfit = r.profit)
    }

    suspend fun duplicatePiece(piece: PieceEntity, copySuffix: String): Long {
        val image = piece.image?.let { ref ->
            images.fileOf(ref)?.takeIf { it.exists() }?.let { images.saveBytes(it.readBytes()) } ?: ref
        }
        return db.pieces().upsert(
            piece.copy(id = 0, name = piece.name + copySuffix, image = image,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deletePiece(piece: PieceEntity) {
        db.withTransaction {
            db.orders().detachPiece(piece.id)
            db.pieces().delete(piece)
        }
        images.delete(piece.image)
    }

    suspend fun newOrder(title: String = ""): Long =
        db.orders().insert(OrderEntity(number = db.orders().nextNumber(), title = title))

    suspend fun addPieceToOrder(orderId: Long, pieceId: Long, quantity: Int = 1) {
        val piece = db.pieces().byId(pieceId) ?: return
        val fresh = recalculate(piece)
        val position = db.orders().lines(orderId).size
        db.orders().insertLine(
            OrderLineEntity(
                orderId = orderId,
                pieceId = piece.id,
                description = piece.name,
                quantity = quantity.coerceAtLeast(1),
                unitPrice = fresh.unitPrice,
                unitCost = fresh.unitCost,
                unitProfit = fresh.unitProfit,
                vatPercent = piece.vatPercent,
                materialsPerUnit = PieceMapper.materialsPerUnit(piece),
                position = position
            )
        )
        touch(orderId)
    }

    /** Vuelve a calcular los precios de las líneas con el catálogo de hoy. */
    suspend fun refreshOrderPrices(orderId: Long) {
        db.withTransaction {
            db.orders().lines(orderId).forEach { line ->
                val piece = line.pieceId?.let { db.pieces().byId(it) } ?: return@forEach
                val fresh = recalculate(piece)
                db.orders().updateLine(
                    line.copy(
                        description = piece.name,
                        unitPrice = fresh.unitPrice,
                        unitCost = fresh.unitCost,
                        unitProfit = fresh.unitProfit,
                        vatPercent = piece.vatPercent,
                        materialsPerUnit = PieceMapper.materialsPerUnit(piece)
                    )
                )
            }
            touch(orderId)
        }
    }

    suspend fun updateOrder(order: OrderEntity) = db.orders().update(order.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateLine(line: OrderLineEntity) {
        db.orders().updateLine(line)
        touch(line.orderId)
    }

    suspend fun removeLine(line: OrderLineEntity) {
        db.orders().deleteLine(line.id)
        touch(line.orderId)
    }

    /**
     * Cambia el estado. Al pasar a impreso o entregado descuenta el filamento del stock una sola vez;
     * al volver a borrador, enviado o aceptado lo devuelve. Devuelve lo descontado (+) o devuelto (−).
     */
    suspend fun setStatus(orderId: Long, status: OrderStatus): Map<Long, BigDecimal> = db.withTransaction {
        val order = db.orders().byId(orderId) ?: return@withTransaction emptyMap()
        val consumes = status == OrderStatus.PRINTED || status == OrderStatus.DELIVERED
        val returns = !consumes && status != OrderStatus.CANCELLED
        var deducted = order.stockDeducted
        val moved = mutableMapOf<Long, BigDecimal>()
        if (consumes != order.stockDeducted && (consumes || returns)) {
            val sign = if (consumes) BigDecimal.ONE else BigDecimal.ONE.negate()
            OrderMath.materialConsumption(db.orders().lines(orderId)).forEach { (materialId, amount) ->
                val m = db.materials().byId(materialId) ?: return@forEach
                if (!m.trackStock) return@forEach
                db.materials().update(m.copy(stock = (m.stock - amount * sign).max(BigDecimal.ZERO)))
                moved[materialId] = amount * sign
            }
            deducted = consumes
        }
        db.orders().update(order.copy(status = status, stockDeducted = deducted, updatedAt = System.currentTimeMillis()))
        moved
    }

    suspend fun deleteOrder(orderId: Long) = db.orders().delete(orderId)

    private suspend fun touch(orderId: Long) {
        db.orders().byId(orderId)?.let { db.orders().update(it.copy(updatedAt = System.currentTimeMillis())) }
    }
}
