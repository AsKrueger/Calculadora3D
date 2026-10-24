package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.domain.model.QuoteStatus
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.UiState
import com.tdcostmanager.app.ui.material.InfoBox
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    projectId: Long,
    quoteId: Long,
    viewModel: QuoteViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.quoteDetailState.collectAsState()
    val statusUpdateState by viewModel.statusUpdateState.collectAsState()

    LaunchedEffect(projectId, quoteId) {
        viewModel.loadQuoteDetail(projectId, quoteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Presupuesto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val quote = s.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(text = "Presupuesto #${quote.id}", style = MaterialTheme.typography.headlineMedium)
                        Text(text = "Estado: ${quote.status.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        quote.calculatedAt?.let {
                            Text(text = "Calculado para: ${formatInstant(it)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Precio Final", value = formatEuros(quote.finalPrice), modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        CostBreakdown(quote)

                        Spacer(modifier = Modifier.height(16.dp))

                        StatusActions(
                            status = quote.status,
                            isUpdating = statusUpdateState is UiState.Loading,
                            onStatusSelected = { viewModel.updateStatus(projectId, quoteId, it) }
                        )

                        val updateError = statusUpdateState
                        if (updateError is UiState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = updateError.error.toFriendlyMessage(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is UiState.Error -> Text(s.error.toFriendlyMessage(), modifier = Modifier.align(Alignment.Center).padding(16.dp))
                else -> {}
            }
        }
    }
}

@Composable
private fun CostBreakdown(quote: QuoteResponse) {
    Text(text = "Desglose", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    // Los presupuestos antiguos no guardaban el desglose por partida
    if (quote.materialCost == null) {
        Text(
            text = "Este presupuesto se generó antes de guardar el desglose por partida.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        BreakdownRow("Materiales", quote.materialCost)
        BreakdownRow("Máquinas", quote.machineCost)
        BreakdownRow("Herramientas", quote.toolCost)
        BreakdownRow("Mano de obra", quote.laborCost)
        BreakdownRow("Electricidad", quote.electricityCost)
        quote.energyKWh?.takeIf { it > 0 }?.let { energy ->
            val price = quote.electricityPricePerKWh?.let { " a ${String.format(Locale.getDefault(), "%.5f", it)} €/kWh" } ?: ""
            Text(
                text = "${String.format(Locale.getDefault(), "%.3f", energy)} kWh$price",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }

    BreakdownRow("Coste base", quote.baseCost)
    BreakdownRow("Seguridad (${quote.safetyPercentage}%)", quote.safetyAmount)
    BreakdownRow("Coste ajustado", quote.adjustedCost)
    BreakdownRow("Beneficio (${quote.marginPercentage}%)", quote.profitAmount)
    BreakdownRow("Precio final", quote.finalPrice)
}

@Composable
private fun BreakdownRow(label: String, value: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value?.let { formatEuros(it) } ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusActions(
    status: QuoteStatus,
    isUpdating: Boolean,
    onStatusSelected: (QuoteStatus) -> Unit
) {
    val transitions = status.allowedTransitions
    if (transitions.isEmpty()) return

    Text(text = "Cambiar estado", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        transitions.forEach { target ->
            OutlinedButton(onClick = { onStatusSelected(target) }, enabled = !isUpdating) {
                Text(target.label)
            }
        }
    }
    if (isUpdating) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

private fun formatEuros(value: Double): String = String.format(Locale.getDefault(), "%.2f €", value)

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatInstant(isoInstant: String): String =
    runCatching { Instant.parse(isoInstant).atZone(ZoneId.systemDefault()).format(dateTimeFormatter) }
        .getOrDefault(isoInstant)
