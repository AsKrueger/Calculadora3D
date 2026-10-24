package com.tdcostmanager.app.ui.tool

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone

@Composable
fun ToolDetailScreen(
    id: Long,
    viewModel: ToolViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.toolDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var confirmDeactivate by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.loadToolDetail(id) }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val tool = (state as? UiState.Success)?.data
    Scaffold(
        topBar = {
            AppTopBar("Herramienta", onBack = onNavigateBack) {
                if (tool != null) {
                    IconButton(onClick = { onEditClick(tool.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (tool.active) {
                        IconButton(onClick = { confirmDeactivate = true }) {
                            Icon(Icons.Default.Block, contentDescription = "Desactivar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is UiState.Loading, UiState.Idle -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadToolDetail(id) }, modifier = Modifier.padding(padding))
            is UiState.Success -> DetailColumn(padding) {
                val t = s.data
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Handyman, size = 56)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        StatusChip(if (t.active) "Activa" else "Inactiva", if (t.active) Tone.Success else Tone.Neutral)
                    }
                }
                SectionCard(title = "Coste por uso") {
                    InfoRow("Amortización", "${Format.number(if (t.estimatedUses > 0) t.acquisitionCost / t.estimatedUses else 0.0, 4)} €")
                    InfoRow("Mantenimiento", Format.percent(t.maintenancePercentage))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Total por uso", "${Format.number(t.costPerUse, 4)} €", emphasize = true)
                }
                SectionCard(title = "Datos de la herramienta") {
                    InfoRow("Precio de compra", Format.euros(t.acquisitionCost))
                    InfoRow("Usos estimados", Format.number(t.estimatedUses, 0))
                }
                if (!t.description.isNullOrBlank()) {
                    SectionCard(title = "Descripción") {
                        Text(t.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                (operationState as? UiState.Error)?.let { ErrorBanner(it.error.toFriendlyMessage()) }
            }
        }
    }

    if (confirmDeactivate && tool != null) {
        ConfirmDialog(
            title = "Desactivar herramienta",
            text = "«${tool.name}» quedará marcada como inactiva. Los proyectos que ya la usan no cambian.",
            confirmLabel = "Desactivar",
            onConfirm = { viewModel.deactivateTool(tool.id) },
            onDismiss = { confirmDeactivate = false }
        )
    }
}
