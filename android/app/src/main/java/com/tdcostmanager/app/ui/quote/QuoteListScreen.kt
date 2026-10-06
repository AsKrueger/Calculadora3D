package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteListScreen(
    projectId: Long,
    viewModel: QuoteViewModel,
    onQuoteClick: (Long) -> Unit,
    onCreateQuote: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.quotesState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadQuotes(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos (Quotes)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateQuote) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Presupuesto")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val quotes = s.data
                    if (quotes.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No hay presupuestos emitidos.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onCreateQuote) {
                                Text("Generar Primer Presupuesto")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(quotes) { quote ->
                                QuoteItem(quote = quote, onClick = { onQuoteClick(quote.id) })
                            }
                        }
                    }
                }
                is UiState.Error -> Button(onClick = { viewModel.loadQuotes(projectId) }, modifier = Modifier.align(Alignment.Center)) { Text("Reintentar") }
                else -> {}
            }
        }
    }
}

@Composable
fun QuoteItem(quote: QuoteResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Presupuesto #${quote.id}", style = MaterialTheme.typography.titleMedium)
                Text(text = "${quote.finalPrice} €", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Base: ${quote.baseCost} € | Ajustado: ${quote.adjustedCost} €",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
