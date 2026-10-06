package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.layout.*
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
import com.tdcostmanager.app.ui.common.UiState
import com.tdcostmanager.app.ui.material.InfoBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    projectId: Long,
    quoteId: Long,
    viewModel: QuoteViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.quoteDetailState.collectAsState()

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
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = "Presupuesto #${quote.id}", style = MaterialTheme.typography.headlineMedium)
                        Text(text = "Estado: ${quote.status}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Coste Base", value = "${quote.baseCost} €", modifier = Modifier.weight(1f))
                            InfoBox(label = "Coste Ajustado", value = "${quote.adjustedCost} €", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Precio Final", value = "${quote.finalPrice} €", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Margen Beneficio", value = "${quote.marginPercentage}%", modifier = Modifier.weight(1f))
                            InfoBox(label = "Margen Seguridad", value = "${quote.safetyPercentage}%", modifier = Modifier.weight(1f))
                        }
                    }
                }
                is UiState.Error -> Text("Error al cargar presupuesto", modifier = Modifier.align(Alignment.Center))
                else -> {}
            }
        }
    }
}
