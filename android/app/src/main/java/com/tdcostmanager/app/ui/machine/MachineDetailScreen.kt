package com.tdcostmanager.app.ui.machine

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone

@Composable
fun MachineDetailScreen(
    id: Long,
    viewModel: MachineViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.machineDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var confirmDeactivate by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.loadMachineDetail(id) }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val machine = (state as? UiState.Success)?.data
    Scaffold(
        topBar = {
            AppTopBar("Máquina", onBack = onNavigateBack) {
                if (machine != null) {
                    IconButton(onClick = { onEditClick(machine.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (machine.active) {
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
            is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadMachineDetail(id) }, modifier = Modifier.padding(padding))
            is UiState.Success -> DetailColumn(padding) {
                val m = s.data
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Print, size = 56)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        StatusChip(if (m.active) "Activa" else "Inactiva", if (m.active) Tone.Success else Tone.Neutral)
                    }
                }
                SectionCard(title = "Coste por hora de impresión") {
                    InfoRow("Amortización", "${Format.number(if (m.usefulLifeHours > 0) m.acquisitionCost / m.usefulLifeHours else 0.0, 4)} €/h")
                    InfoRow("Mantenimiento", "${Format.number(m.maintenanceCostPerHour, 4)} €/h")
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow("Total sin electricidad", "${Format.number(m.hourlyCost, 4)} €/h", emphasize = true)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "La electricidad se suma al presupuestar: ${Format.energy(m.powerWatts / 1000)} por hora al precio de ESIOS de ese momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SectionCard(title = "Datos de la máquina") {
                    InfoRow("Precio de compra", Format.euros(m.acquisitionCost))
                    InfoRow("Vida útil", Format.hours(m.usefulLifeHours))
                    InfoRow("Potencia", "${Format.number(m.powerWatts)} W")
                }
                (operationState as? UiState.Error)?.let { ErrorBanner(it.error.toFriendlyMessage()) }
            }
        }
    }

    if (confirmDeactivate && machine != null) {
        ConfirmDialog(
            title = "Desactivar máquina",
            text = "«${machine.name}» quedará marcada como inactiva. Los proyectos que ya la usan no cambian.",
            confirmLabel = "Desactivar",
            onConfirm = { viewModel.deactivateMachine(machine.id) },
            onDismiss = { confirmDeactivate = false }
        )
    }
}
