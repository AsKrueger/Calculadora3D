package com.tdcostmanager.app.ui.quote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

private val marginPresets = listOf(10.0, 20.0, 30.0, 50.0)
private val safetyPresets = listOf(0.0, 5.0, 10.0, 15.0)

@Composable
fun QuoteCalculatorScreen(
    projectId: Long,
    viewModel: QuoteViewModel,
    onNavigateBack: () -> Unit,
    onQuoteCreated: (Long) -> Unit
) {
    val createdState by viewModel.createdQuoteState.collectAsState()
    var margin by rememberSaveable { mutableStateOf("20") }
    var safety by rememberSaveable { mutableStateOf("5") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(createdState) {
        val s = createdState
        if (s is UiState.Success) {
            viewModel.resetCreatedQuoteState()
            onQuoteCreated(s.data.id)
        }
    }

    val marginError = if (submitted) validateNumber(margin, max = 100.0) else null
    val safetyError = if (submitted) validateNumber(safety, max = 100.0) else null

    FormScaffold(
        title = "Nuevo presupuesto",
        onBack = onNavigateBack,
        saveLabel = "Calcular presupuesto",
        saving = createdState is UiState.Loading,
        error = (createdState as? UiState.Error)?.error?.toFriendlyMessage(),
        onSave = {
            submitted = true
            val m = Format.parseDecimal(margin)
            val sf = Format.parseDecimal(safety)
            if (validateNumber(margin, max = 100.0) == null && validateNumber(safety, max = 100.0) == null && m != null && sf != null) {
                viewModel.createQuote(projectId, m, sf)
            }
        }
    ) {
        PercentCard(
            title = "Margen de seguridad",
            explanation = "Colchón sobre el coste para imprevistos: impresiones fallidas, purgas, desperdicio.",
            value = safety,
            onValueChange = { safety = it },
            presets = safetyPresets,
            error = safetyError
        )
        PercentCard(
            title = "Margen de beneficio",
            explanation = "Lo que ganas. Se aplica sobre el coste ya ajustado con la seguridad.",
            value = margin,
            onValueChange = { margin = it },
            presets = marginPresets,
            error = marginError,
            imeAction = ImeAction.Done
        )
        Text(
            "La electricidad de las máquinas se valora con el precio de ESIOS para la hora actual. " +
                "El presupuesto se guarda con su desglose y queda en estado «Borrador».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PercentCard(
    title: String,
    explanation: String,
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<Double>,
    error: String?,
    imeAction: ImeAction = ImeAction.Next
) {
    SectionCard(title = title) {
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = Format.parseDecimal(value) == preset,
                    onClick = { onValueChange(Format.forInput(preset)) },
                    label = { Text(Format.percent(preset)) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        NumberField(value, onValueChange, "Porcentaje", suffix = "%", error = error, imeAction = imeAction)
    }
}
