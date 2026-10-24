package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.domain.model.QuoteStatus
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.tabular

@Composable
fun QuoteDetailScreen(
    projectId: Long,
    quoteId: Long,
    viewModel: QuoteViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.quoteDetailState.collectAsState()
    val statusUpdateState by viewModel.statusUpdateState.collectAsState()

    LaunchedEffect(projectId, quoteId) { viewModel.loadQuoteDetail(projectId, quoteId) }

    Scaffold(topBar = { AppTopBar("Presupuesto #$quoteId", onBack = onNavigateBack) }) { padding ->
        when (val s = state) {
            is UiState.Loading, UiState.Idle -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadQuoteDetail(projectId, quoteId) }, modifier = Modifier.padding(padding))
            is UiState.Success -> DetailColumn(padding) {
                val quote = s.data
                PriceHero(quote)
                CostBreakdown(quote)
                StatusActions(
                    status = quote.status,
                    isUpdating = statusUpdateState is UiState.Loading,
                    onStatusSelected = { viewModel.updateStatus(projectId, quoteId, it) }
                )
                (statusUpdateState as? UiState.Error)?.let { ErrorBanner(it.error.toFriendlyMessage()) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PriceHero(quote: QuoteResponse) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Precio final", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            StatusChip(quote.status.label, quote.status.tone)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            Format.euros(quote.finalPrice),
            style = MaterialTheme.typography.displaySmall.tabular(),
            color = MaterialTheme.colorScheme.primary
        )
        quote.calculatedAt?.let {
            Text(
                "Calculado para el ${Format.dateTime(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class Part(val label: String, val amount: Double)

@Composable
private fun CostBreakdown(quote: QuoteResponse) {
    SectionCard(title = "Desglose del coste") {
        // Los presupuestos anteriores a la Fase 3 no guardaban las partidas
        if (quote.materialCost == null) {
            Text(
                "Este presupuesto se generó antes de guardar el desglose por partida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        } else {
            val parts = listOf(
                Part("Materiales", quote.materialCost),
                Part("Máquinas", quote.machineCost ?: 0.0),
                Part("Herramientas", quote.toolCost ?: 0.0),
                Part("Mano de obra", quote.laborCost ?: 0.0),
                Part("Electricidad", quote.electricityCost ?: 0.0)
            )
            val colors = AppThemeExtras.breakdownColors
            val total = parts.sumOf { it.amount }
            if (total > 0) {
                StackedBar(parts.map { it.amount / total }, colors)
                Spacer(Modifier.height(12.dp))
            }
            parts.forEachIndexed { i, part ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(colors[i]))
                    Spacer(Modifier.width(10.dp))
                    Text(part.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (total > 0) {
                        Text(
                            "${Format.number(part.amount / total * 100, 0)} %",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(Format.euros(part.amount), style = MaterialTheme.typography.bodyMedium.tabular())
                }
            }
            val energy = quote.energyKWh
            if (energy != null && energy > 0) {
                Text(
                    "${Format.energy(energy)}" + (quote.electricityPricePerKWh?.let { " a ${Format.pricePerKWh(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("Coste base", Format.euros(quote.baseCost))
        InfoRow("Seguridad (${Format.percent(quote.safetyPercentage)})", quote.safetyAmount?.let { "+ ${Format.euros(it)}" } ?: "—")
        InfoRow("Coste ajustado", Format.euros(quote.adjustedCost))
        InfoRow("Beneficio (${Format.percent(quote.marginPercentage)})", quote.profitAmount?.let { "+ ${Format.euros(it)}" } ?: "—")
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("Precio final", Format.euros(quote.finalPrice), emphasize = true)
    }
}

/** Barra horizontal dividida en tramos proporcionales a cada partida. */
@Composable
private fun StackedBar(fractions: List<Double>, colors: List<Color>) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        fractions.forEachIndexed { i, f ->
            if (f > 0) Box(Modifier.weight(f.toFloat()).fillMaxHeight().background(colors[i]))
        }
    }
}

private val QuoteStatus.actionLabel: String
    get() = when (this) {
        QuoteStatus.DRAFT -> "Borrador"
        QuoteStatus.SENT -> "Marcar como enviado"
        QuoteStatus.ACCEPTED -> "Aceptado"
        QuoteStatus.REJECTED -> "Rechazado"
        QuoteStatus.EXPIRED -> "Caducado"
    }

@Composable
private fun StatusActions(status: QuoteStatus, isUpdating: Boolean, onStatusSelected: (QuoteStatus) -> Unit) {
    val transitions = status.allowedTransitions
    SectionCard(title = "Estado") {
        if (transitions.isEmpty()) {
            Text(
                "Estado final: «${status.label}». Para ofrecer otro precio, calcula un presupuesto nuevo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@SectionCard
        }
        Text(
            if (status == QuoteStatus.DRAFT) "Cuando se lo envíes al cliente, márcalo como enviado." else "¿Qué ha respondido el cliente?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        val (primary, others) = transitions.partition { it == QuoteStatus.SENT || it == QuoteStatus.ACCEPTED }
        primary.forEach {
            PrimaryButton(text = it.actionLabel, onClick = { onStatusSelected(it) }, loading = isUpdating)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            others.forEach {
                OutlinedButton(
                    onClick = { onStatusSelected(it) },
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.small
                ) { Text(it.actionLabel) }
            }
        }
    }
}
