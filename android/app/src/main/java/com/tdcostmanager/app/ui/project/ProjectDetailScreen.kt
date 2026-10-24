package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

@Composable
fun ProjectDetailScreen(
    id: Long,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onNavigateToQuotes: (Long) -> Unit,
    onNavigateToCalculator: (Long) -> Unit
) {
    val state by viewModel.projectDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var confirmArchive by remember { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.loadProjectDetail(id) }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val project = (state as? UiState.Success)?.data
    val editable = project != null && project.status != ProjectStatus.ARCHIVED

    Scaffold(
        topBar = {
            AppTopBar("Proyecto", onBack = onNavigateBack) {
                if (editable) {
                    IconButton(onClick = { onEditClick(project!!.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { confirmArchive = true }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archivar")
                    }
                }
            }
        },
        bottomBar = {
            if (project != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = ScreenPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToQuotes(project.id) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Presupuestos")
                        }
                        if (editable) {
                            PrimaryButton(
                                text = "Presupuestar",
                                onClick = { onNavigateToCalculator(project.id) },
                                icon = Icons.Default.Calculate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is UiState.Loading, UiState.Idle -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.error, onRetry = { viewModel.loadProjectDetail(id) }, modifier = Modifier.padding(padding))
            is UiState.Success -> DetailColumn(padding) {
                (operationState as? UiState.Error)?.let { ErrorBanner(it.error.toFriendlyMessage()) }
                ProjectDetailContent(s.data)
            }
        }
    }

    if (confirmArchive && project != null) {
        ConfirmDialog(
            title = "Archivar proyecto",
            text = "«${project.name}» dejará de poder editarse y presupuestarse. Sus presupuestos se conservan.",
            confirmLabel = "Archivar",
            onConfirm = { viewModel.archiveProject(project.id) },
            onDismiss = { confirmArchive = false }
        )
    }
}

@Composable
private fun ColumnScope.ProjectDetailContent(project: ProjectResponse) {
    Column {
        StatusChip(project.status.label, project.status.tone)
        Spacer(Modifier.height(10.dp))
        Text(project.name, style = MaterialTheme.typography.headlineSmall)
        if (!project.description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (project.status == ProjectStatus.ARCHIVED) {
        SectionCard {
            Text(
                "Proyecto archivado: no se puede editar ni generar nuevos presupuestos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    SectionCard(title = "Mano de obra") {
        Row {
            MetricTile("Horas", Format.hours(project.laborHours), Modifier.weight(1f))
            MetricTile("Precio/hora", Format.euros(project.laborCostPerHour), Modifier.weight(1f))
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("Coste de mano de obra", Format.euros(project.laborHours * project.laborCostPerHour), emphasize = true)
    }

    LinesCard(
        title = "Materiales",
        icon = Icons.Default.Inventory2,
        lines = project.materials.map { it.name to Format.quantity(it.quantityUsed, it.unit) },
        empty = "Sin materiales"
    )
    LinesCard(
        title = "Máquinas",
        icon = Icons.Default.Print,
        lines = project.machines.map { it.name to Format.hours(it.estimatedHours) },
        empty = "Sin máquinas"
    )
    LinesCard(
        title = "Herramientas",
        icon = Icons.Default.Handyman,
        lines = project.tools.map { it.name to "${Format.number(it.uses)} ${if (it.uses == 1.0) "uso" else "usos"}" },
        empty = "Sin herramientas"
    )

    Text(
        "Creado el ${Format.dateTime(project.createdAt)} · Actualizado el ${Format.dateTime(project.updatedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LinesCard(title: String, icon: ImageVector, lines: List<Pair<String, String>>, empty: String) {
    SectionCard(
        title = title,
        trailing = { Text("${lines.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        if (lines.isEmpty()) {
            Text(empty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            lines.forEachIndexed { index, (name, amount) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    InfoRow(name, amount, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
