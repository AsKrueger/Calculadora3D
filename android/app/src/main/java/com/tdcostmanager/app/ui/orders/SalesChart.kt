package com.tdcostmanager.app.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.calc.OrderMath
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone
import java.math.BigDecimal
import java.time.format.TextStyle

/** Ventas y beneficio de los últimos meses: barras agrupadas por mes, el más reciente a la derecha. */
@Composable
fun SalesChart(months: List<OrderMath.MonthSummary>) {
    val fmt = LocalFmt.current
    val salesColor = MaterialTheme.colorScheme.primary
    val profitColor = AppThemeExtras.tone(Tone.Success).content
    val axis = MaterialTheme.colorScheme.outlineVariant
    val max = months.maxOf { maxOf(it.sales, it.profit, BigDecimal.ZERO) }.toFloat().coerceAtLeast(1f)
    val description = months.joinToString("; ") {
        it.month.month.getDisplayName(TextStyle.FULL, fmt.locale) + ": " + fmt.money(it.sales) + ", " + fmt.money(it.profit)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chart_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Legend(salesColor, stringResource(R.string.summary_sales))
                Spacer(Modifier.width(12.dp))
                Legend(profitColor, stringResource(R.string.summary_profit))
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = description }
            ) {
                val slot = size.width / months.size
                val barW = (slot * 0.28f).coerceAtMost(18.dp.toPx())
                val gap = 3.dp.toPx()
                months.forEachIndexed { i, m ->
                    val cx = slot * i + slot / 2
                    fun bar(value: BigDecimal, x: Float, color: Color) {
                        val h = (value.toFloat().coerceAtLeast(0f) / max) * size.height
                        if (h <= 0f) return
                        drawRoundRect(
                            color, Offset(x, size.height - h), Size(barW, h),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                    bar(m.sales, cx - barW - gap / 2, salesColor)
                    bar(m.profit, cx + gap / 2, profitColor)
                }
                drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            Row(Modifier.fillMaxWidth()) {
                months.forEach {
                    Text(
                        it.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, fmt.locale).trimEnd('.'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
