package com.tdcostmanager.app.core

import android.content.Context
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.data.db.AppDatabase
import com.tdcostmanager.app.data.db.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pedidos y piezas en CSV para la hoja de cálculo o la gestoría. Con coma decimal (español, francés…)
 * el separador es «;», como espera Excel en esos idiomas; el BOM hace que Excel lea bien las tildes.
 */
object CsvExport {

    /** Separador y formato de número según el idioma; aislado para poder probarlo. */
    class Format(locale: java.util.Locale) {
        private val decimal = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val separator = if (decimal == ',') ';' else ','

        fun num(v: BigDecimal, scale: Int = 2): String =
            v.setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', decimal)

        fun cell(text: String): String =
            if (text.any { it == separator || it == '"' || it == '\n' || it == '\r' })
                "\"" + text.replace("\"", "\"\"") + "\"" else text

        fun row(cells: List<String>) = cells.joinToString(separator.toString()) + "\r\n"
    }

    suspend fun export(context: Context, db: AppDatabase, locale: java.util.Locale): List<File> = withContext(Dispatchers.IO) {
        val r = context.resources
        val f = Format(locale)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        fun date(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toString()

        val statusNames = mapOf(
            OrderStatus.DRAFT to R.string.status_draft, OrderStatus.SENT to R.string.status_sent,
            OrderStatus.ACCEPTED to R.string.status_accepted, OrderStatus.PRINTED to R.string.status_printed,
            OrderStatus.DELIVERED to R.string.status_delivered, OrderStatus.CANCELLED to R.string.status_cancelled
        ).mapValues { r.getString(it.value) }

        val orders = StringBuilder("﻿")
        orders.append(f.row(r.getStringArray(R.array.csv_orders_header).toList()))
        db.orders().observeAll().first().sortedBy { it.order.number }.forEach { o ->
            val t = OrderMath.totals(o.order, o.lines)
            orders.append(
                f.row(
                    listOf(
                        o.order.number.toString(), date(o.order.createdAt), f.cell(statusNames.getValue(o.order.status)),
                        f.cell(o.order.title), f.cell(o.order.clientName), t.units.toString(),
                        f.num(t.subtotal), f.num(t.discount), f.num(t.shipping), f.num(t.total), f.num(t.cost), f.num(t.profit)
                    )
                )
            )
        }

        val printers = db.printers().all().associateBy { it.id }
        val pieces = StringBuilder("﻿")
        pieces.append(f.row(r.getStringArray(R.array.csv_pieces_header).toList()))
        db.pieces().all().sortedBy { it.name.lowercase() }.forEach { p ->
            pieces.append(
                f.row(
                    listOf(
                        f.cell(p.name), f.cell(p.printerId?.let { printers[it]?.name } ?: ""), f.num(p.printHours),
                        p.piecesPerPlate.toString(),
                        f.num(p.materials.fold(BigDecimal.ZERO) { a, m -> a + m.amount }, 1),
                        f.num(p.unitCost), f.num(p.unitPrice), f.num(p.unitProfit), date(p.updatedAt)
                    )
                )
            )
        }

        listOf(
            File(dir, r.getString(R.string.csv_orders_file, today.toString())).apply { writeText(orders.toString()) },
            File(dir, r.getString(R.string.csv_pieces_file, today.toString())).apply { writeText(pieces.toString()) }
        )
    }
}
