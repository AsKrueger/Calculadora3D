package com.tdcostmanager.app.ui.material

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone

@Composable
fun MaterialDetailScreen(
    id: Long,
    viewModel: MaterialViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.materialDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var confirmDeactivate by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.loadMaterialDetail(id) }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val material = (state as? UiState.Success)?.data
    Scaffold(
        topBar = {
            AppTopBar("Material", onBack = onNavigateBack) {
                if (material != null) {
                    IconButton(onClick = { onEditClick(material.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (material.active) {
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
            is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadMaterialDetail(id) }, modifier = Modifier.padding(padding))
            is UiState.Success -> DetailColumn(padding) {
                val m = s.data
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(m.category.icon, size = 56)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(m.category.label, Tone.Progress)
                            StatusChip(if (m.active) "Activo" else "Inactivo", if (m.active) Tone.Success else Tone.Neutral)
                        }
                    }
                }
                SectionCard(title = "Compra") {
                    Row {
                        MetricTile("Precio", Format.euros(m.purchasePrice), Modifier.weight(1f))
                        MetricTile("Cantidad", Format.quantity(m.quantity, m.unit), Modifier.weight(1f))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Coste por ${m.unit.symbol}", m.unitCostLabel(), emphasize = true)
                }
                if (!m.description.isNullOrBlank()) {
                    SectionCard(title = "Descripción") {
                        Text(m.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                (operationState as? UiState.Error)?.let { ErrorBanner(it.error.toFriendlyMessage()) }
            }
        }
    }

    if (confirmDeactivate && material != null) {
        ConfirmDialog(
            title = "Desactivar material",
            text = "«${material.name}» quedará marcado como inactivo. Los proyectos que ya lo usan no cambian.",
            confirmLabel = "Desactivar",
            onConfirm = { viewModel.deactivateMaterial(material.id) },
            onDismiss = { confirmDeactivate = false }
        )
    }
}
