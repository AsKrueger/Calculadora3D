package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

// Archivar tiene su propia acción, con confirmación, en el detalle del proyecto
private val editableStatuses = ProjectStatus.entries - ProjectStatus.ARCHIVED

@Composable
fun ProjectFormScreen(
    id: Long?,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.projectDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val catalogState by viewModel.catalogState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var laborHours by rememberSaveable { mutableStateOf("") }
    var laborCostPerHour by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(ProjectStatus.DRAFT) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    val materialLines = remember { mutableStateListOf<MaterialLine>() }
    val machineLines = remember { mutableStateListOf<MachineLine>() }
    val toolLines = remember { mutableStateListOf<ToolLine>() }

    LaunchedEffect(id) {
        viewModel.loadCatalog()
        if (id != null) viewModel.loadProjectDetail(id)
    }
    LaunchedEffect(detailState) {
        val s = detailState
        if (id != null && !loaded && s is UiState.Success) {
            val p = s.data
            name = p.name
            description = p.description.orEmpty()
            laborHours = Format.forInput(p.laborHours)
            laborCostPerHour = Format.forInput(p.laborCostPerHour)
            status = p.status
            materialLines.addAll(p.materials.map { MaterialLine(it.materialId, Format.forInput(it.quantityUsed), it.unit) })
            machineLines.addAll(p.machines.map { MachineLine(it.machineId, Format.forInput(it.estimatedHours)) })
            toolLines.addAll(p.tools.map { ToolLine(it.toolId, Format.forInput(it.uses)) })
            loaded = true
        }
    }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    val hoursError = if (submitted) validateNumber(laborHours) else null
    val rateError = if (submitted) validateNumber(laborCostPerHour) else null
    val hours = Format.parseDecimal(laborHours)
    val rate = Format.parseDecimal(laborCostPerHour)

    FormScaffold(
        title = if (id != null) "Editar proyecto" else "Nuevo proyecto",
        onBack = onNavigateBack,
        saveLabel = if (id != null) "Guardar cambios" else "Crear proyecto",
        saving = operationState is UiState.Loading,
        error = (operationState as? UiState.Error)?.error?.toFriendlyMessage(),
        onSave = {
            submitted = true
            val linesValid = materialLines.all { it.error() == null } &&
                machineLines.all { it.error() == null } &&
                toolLines.all { it.error() == null }
            if (name.isNotBlank() && validateNumber(laborHours) == null && validateNumber(laborCostPerHour) == null &&
                hours != null && rate != null && linesValid
            ) {
                val desc = description.ifBlank { null }
                val materials = materialLines.map { it.toRequest() }
                val machines = machineLines.map { it.toRequest() }
                val tools = toolLines.map { it.toRequest() }
                if (id != null) {
                    viewModel.updateProject(id, ProjectUpdateRequest(name.trim(), desc, status, hours, rate, materials, machines, tools))
                } else {
                    viewModel.createProject(ProjectCreateRequest(name.trim(), desc, hours, rate, materials, machines, tools))
                }
            }
        }
    ) {
        SectionCard(title = "Proyecto") {
            AppTextField(name, { name = it }, "Nombre", error = if (submitted && name.isBlank()) "Obligatorio" else null)
            Spacer(Modifier.height(4.dp))
            AppTextField(description, { description = it }, "Descripción (opcional)", singleLine = false, minLines = 3)
            if (id != null) {
                Spacer(Modifier.height(12.dp))
                DropdownField("Estado", editableStatuses, status, { it.label }, { status = it })
            }
        }
        SectionCard(title = "Mano de obra") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(laborHours, { laborHours = it }, "Horas", suffix = "h", error = hoursError, modifier = Modifier.weight(1f))
                NumberField(laborCostPerHour, { laborCostPerHour = it }, "Precio/hora", suffix = "€/h", error = rateError, modifier = Modifier.weight(1f))
            }
            if (hours != null && rate != null) {
                Text(
                    "Coste de mano de obra: ${Format.euros(hours * rate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        when (val c = catalogState) {
            is UiState.Success -> {
                MaterialLinesCard(
                    lines = materialLines,
                    catalog = c.data.materials,
                    showErrors = submitted,
                    onChange = { i, line -> materialLines[i] = line },
                    onAdd = { materialLines.add(MaterialLine()) },
                    onRemove = { materialLines.removeAt(it) }
                )
                MachineLinesCard(
                    lines = machineLines,
                    catalog = c.data.machines,
                    showErrors = submitted,
                    onChange = { i, line -> machineLines[i] = line },
                    onAdd = { machineLines.add(MachineLine()) },
                    onRemove = { machineLines.removeAt(it) }
                )
                ToolLinesCard(
                    lines = toolLines,
                    catalog = c.data.tools,
                    showErrors = submitted,
                    onChange = { i, line -> toolLines[i] = line },
                    onAdd = { toolLines.add(ToolLine()) },
                    onRemove = { toolLines.removeAt(it) }
                )
            }
            is UiState.Error -> SectionCard {
                Text("No se ha podido cargar el catálogo: ${c.error.toFriendlyMessage()}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = viewModel::loadCatalog) { Text("Reintentar") }
            }
            else -> SectionCard { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
    }
}
