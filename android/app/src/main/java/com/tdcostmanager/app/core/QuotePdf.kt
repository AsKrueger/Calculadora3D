package com.tdcostmanager.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.settings.BusinessInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

/**
 * Presupuesto en PDF (A4) listo para enviar por WhatsApp o correo.
 * [context] debe estar en el idioma de la app para que los textos salgan traducidos.
 */
class QuotePdf(private val context: Context) {

    private val pageW = 595
    private val pageH = 842
    private val margin = 48f

    private val ink = 0xFF15191E.toInt()
    private val muted = 0xFF56606B.toInt()
    private val accent = 0xFFC2410C.toInt()
    private val rule = 0xFFDCE0E5.toInt()

    private fun paint(size: Float, bold: Boolean = false, color: Int = ink) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    suspend fun create(
        order: OrderEntity,
        lines: List<OrderLineEntity>,
        business: BusinessInfo,
        logo: Bitmap?,
        fmt: Fmt
    ): File = withContext(Dispatchers.IO) {
        val r = context.resources
        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        var c: Canvas = page.canvas
        var y = margin

        fun newPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            c = page.canvas
            y = margin
        }

        fun ensure(space: Float) {
            if (y + space > pageH - margin - 40) newPage()
        }

        fun textBlock(text: String, p: TextPaint, x: Float, width: Int): Float {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            c.save()
            c.translate(x, y)
            layout.draw(c)
            c.restore()
            return layout.height.toFloat()
        }

        // Cabecera: logo y datos del negocio a la izquierda, título a la derecha
        val left = margin
        val right = pageW - margin
        var headerBottom = y
        var textX = left
        if (logo != null) {
            val h = 56f
            val w = h * logo.width / logo.height.coerceAtLeast(1)
            c.drawBitmap(logo, null, RectF(left, y, left + w.coerceAtMost(140f), y + h), Paint(Paint.FILTER_BITMAP_FLAG))
            textX = left + w.coerceAtMost(140f) + 14
            headerBottom = y + h
        }
        val bizLines = listOf(business.name).filter { it.isNotBlank() }
        val bizDetails = listOf(business.taxId, business.address, business.phone, business.email).filter { it.isNotBlank() }
        var by = y + 14
        bizLines.forEach { c.drawText(it, textX, by, paint(14f, bold = true)); by += 18 }
        bizDetails.forEach { c.drawText(it, textX, by, paint(9.5f, color = muted)); by += 13 }
        headerBottom = maxOf(headerBottom, by)

        val title = r.getString(R.string.pdf_title, order.number)
        val tp = paint(20f, bold = true, color = accent)
        c.drawText(title, right - tp.measureText(title), y + 18, tp)
        val dateText = fmt.date(order.createdAt)
        val dp = paint(10f, color = muted)
        c.drawText(dateText, right - dp.measureText(dateText), y + 34, dp)
        order.dueDate?.let {
            val due = r.getString(R.string.pdf_due, fmt.date(it))
            c.drawText(due, right - dp.measureText(due), y + 48, dp)
        }
        y = maxOf(headerBottom, y + 56) + 24

        // Cliente
        if (order.clientName.isNotBlank() || order.clientContact.isNotBlank()) {
            c.drawText(r.getString(R.string.pdf_for).uppercase(), left, y, paint(8.5f, bold = true, color = muted))
            y += 15
            if (order.clientName.isNotBlank()) { c.drawText(order.clientName, left, y, paint(12f, bold = true)); y += 15 }
            if (order.clientContact.isNotBlank()) { c.drawText(order.clientContact, left, y, paint(10f, color = muted)); y += 14 }
            y += 12
        }
        if (order.title.isNotBlank()) {
            y += textBlock(order.title, paint(13f, bold = true), left, (right - left).toInt()) + 12
        }

        // Tabla de líneas
        val colQty = right - 210
        val colUnit = right - 120
        fun header() {
            val hp = paint(8.5f, bold = true, color = muted)
            c.drawText(r.getString(R.string.pdf_col_item).uppercase(), left, y, hp)
            listOf(r.getString(R.string.pdf_col_qty) to colQty, r.getString(R.string.pdf_col_unit) to colUnit,
                r.getString(R.string.pdf_col_total) to right).forEach { (t, x) ->
                c.drawText(t.uppercase(), x - hp.measureText(t.uppercase()), y, hp)
            }
            y += 8
            c.drawLine(left, y, right, y, Paint().apply { color = ink; strokeWidth = 1f })
            y += 18
        }
        header()
        val body = paint(10.5f)
        lines.forEach { l ->
            ensure(30f)
            if (y == margin) header()
            val descW = (colQty - left - 60).toInt()
            val h = textBlock(l.description, body, left, descW)
            val baseline = y + 11
            val qty = l.quantity.toString()
            c.drawText(qty, colQty - body.measureText(qty), baseline, body)
            val unit = fmt.money(l.unitPrice)
            c.drawText(unit, colUnit - body.measureText(unit), baseline, body)
            val total = fmt.money(l.unitPrice * BigDecimal(l.quantity))
            val bp = paint(10.5f, bold = true)
            c.drawText(total, right - bp.measureText(total), baseline, bp)
            y += maxOf(h, 14f) + 8
            c.drawLine(left, y - 4, right, y - 4, Paint().apply { color = rule; strokeWidth = 0.7f })
            y += 4
        }

        // Totales
        val t = OrderMath.totals(order, lines)
        ensure(110f)
        y += 8
        fun totalRow(label: String, value: String, strong: Boolean = false) {
            val lp = paint(if (strong) 13f else 10.5f, bold = strong, color = if (strong) ink else muted)
            val vp = paint(if (strong) 15f else 10.5f, bold = strong, color = if (strong) accent else ink)
            c.drawText(label, colUnit - 90, y, lp)
            c.drawText(value, right - vp.measureText(value), y, vp)
            y += if (strong) 24 else 17
        }
        totalRow(r.getString(R.string.pdf_subtotal), fmt.money(t.subtotal))
        if (t.discount.signum() > 0) totalRow(
            r.getString(R.string.pdf_discount, fmt.number(order.discountPercent)), "− " + fmt.money(t.discount)
        )
        if (t.shipping.signum() > 0) totalRow(r.getString(R.string.pdf_shipping), fmt.money(t.shipping))
        c.drawLine(colUnit - 90, y - 10, right, y - 10, Paint().apply { color = ink; strokeWidth = 1f })
        y += 6
        totalRow(r.getString(R.string.pdf_total), fmt.money(t.total), strong = true)
        val vat = lines.maxOfOrNull { it.vatPercent } ?: BigDecimal.ZERO
        if (vat.signum() > 0) {
            val note = r.getString(R.string.pdf_vat_included, fmt.number(vat))
            val np = paint(9f, color = muted)
            c.drawText(note, right - np.measureText(note), y - 6, np)
            y += 8
        }

        // Notas y pie
        val notes = listOf(order.notes, business.quoteFooter).filter { it.isNotBlank() }
        if (notes.isNotEmpty()) {
            y += 18
            notes.forEach { n ->
                ensure(40f)
                y += textBlock(n, paint(10f, color = muted), left, (right - left).toInt()) + 10
            }
        }

        doc.finishPage(page)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val base = if (business.name.isBlank()) title else business.name + " " + order.number
        val safe = base.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_')
        val file = File(dir, "$safe.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        file
    }
}
