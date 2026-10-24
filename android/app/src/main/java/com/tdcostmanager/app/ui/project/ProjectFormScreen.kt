package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var laborHours by rememberSaveable { mutableStateOf("") }
    var laborCostPerHour by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(ProjectStatus.DRAFT) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) { if (id != null) viewModel.loadProjectDetail(id) }
    LaunchedEffect(detailState) {
        val s = detailState
        if (id != null && !loaded && s is UiState.Success) {
            val p = s.data
            name = p.name
            description = p.description.orEmpty()
            laborHours = Format.forInput(p.laborHours)
            laborCostPerHour = Format.forInput(p.laborCostPerHour)
            status = p.status
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
            if (name.isNotBlank() && validateNumber(laborHours) == null && validateNumber(laborCostPerHour) == null && hours != null && rate != null) {
                val desc = description.ifBlank { null }
                if (id != null) {
                    viewModel.updateProject(id, ProjectUpdateRequest(name.trim(), desc, status, hours, rate))
                } else {
                    viewModel.createProject(name.trim(), desc, hours, rate)
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
    }
}
