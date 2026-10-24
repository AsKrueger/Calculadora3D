package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.tabular

@Composable
fun QuoteListScreen(
    projectId: Long,
    viewModel: QuoteViewModel,
    onQuoteClick: (Long) -> Unit,
    onCreateQuote: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.quotesState.collectAsState()
    LaunchedEffect(projectId) { viewModel.loadQuotes(projectId) }

    Scaffold(
        topBar = { AppTopBar("Presupuestos", onBack = onNavigateBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateQuote,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo presupuesto") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading, UiState.Idle -> LoadingState()
                is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadQuotes(projectId) })
                is UiState.Success -> if (s.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Sin presupuestos",
                        message = "Calcula el primero: se guardará con su desglose para que puedas consultarlo cuando quieras.",
                        actionLabel = "Calcular presupuesto",
                        onAction = onCreateQuote
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.data.sortedByDescending { it.id }, key = { it.id }) { quote ->
                            QuoteCard(quote, onClick = { onQuoteClick(quote.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: QuoteResponse, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Presupuesto #${quote.id}", style = MaterialTheme.typography.titleMedium)
                Text(
                    Format.dateTime(quote.calculatedAt ?: quote.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.euros(quote.finalPrice),
                    style = MaterialTheme.typography.titleLarge.tabular(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(quote.status.label, quote.status.tone)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Coste ${Format.euros(quote.baseCost)} · Margen ${Format.percent(quote.marginPercentage)} · Seguridad ${Format.percent(quote.safetyPercentage)}",
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
