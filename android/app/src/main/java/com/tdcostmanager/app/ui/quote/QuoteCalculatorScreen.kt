package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteCalculatorScreen(
    projectId: Long,
    viewModel: QuoteViewModel,
    onNavigateBack: () -> Unit,
    onQuoteCreated: (Long) -> Unit
) {
    val createdState by viewModel.createdQuoteState.collectAsState()

    var marginPercentage by remember { mutableStateOf("20.0") }
    var safetyPercentage by remember { mutableStateOf("5.0") }

    LaunchedEffect(createdState) {
        if (createdState is UiState.Success) {
            val quote = (createdState as UiState.Success).data
            viewModel.resetCreatedQuoteState()
            onQuoteCreated(quote.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calcular y Generar Presupuesto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Configuración de Márgenes Económicos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = marginPercentage,
                onValueChange = { marginPercentage = it },
                label = { Text("Margen de Beneficio (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = safetyPercentage,
                onValueChange = { safetyPercentage = it },
                label = { Text("Margen de Seguridad / Imprevistos (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val margin = marginPercentage.toDoubleOrNull() ?: 20.0
                    val safety = safetyPercentage.toDoubleOrNull() ?: 5.0
                    viewModel.createQuote(projectId, margin, safety)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = createdState !is UiState.Loading
            ) {
                if (createdState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Calcular y Emitir Presupuesto (Quote)")
                }
            }

            if (createdState is UiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error al calcular presupuesto",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
