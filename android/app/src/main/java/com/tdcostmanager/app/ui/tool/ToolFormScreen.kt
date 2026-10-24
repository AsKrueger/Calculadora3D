package com.tdcostmanager.app.ui.tool

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.tool.ToolCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolUpdateRequest
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

@Composable
fun ToolFormScreen(
    id: Long?,
    viewModel: ToolViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.toolDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var acquisitionCost by rememberSaveable { mutableStateOf("") }
    var estimatedUses by rememberSaveable { mutableStateOf("") }
    var maintenance by rememberSaveable { mutableStateOf("0") }
    var active by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) { if (id != null) viewModel.loadToolDetail(id) }
    LaunchedEffect(detailState) {
        val s = detailState
        if (id != null && !loaded && s is UiState.Success) {
            val t = s.data
            name = t.name
            description = t.description.orEmpty()
            acquisitionCost = Format.forInput(t.acquisitionCost)
            estimatedUses = Format.forInput(t.estimatedUses)
            maintenance = Format.forInput(t.maintenancePercentage)
            active = t.active
            loaded = true
        }
    }
    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    fun errors() = listOf(
        validateNumber(acquisitionCost),
        validateNumber(estimatedUses, positive = true),
        validateNumber(maintenance, max = 100.0)
    )
    val shown = if (submitted) errors() else List(3) { null }

    FormScaffold(
        title = if (id != null) "Editar herramienta" else "Nueva herramienta",
        onBack = onNavigateBack,
        saveLabel = "Guardar herramienta",
        saving = operationState is UiState.Loading,
        error = (operationState as? UiState.Error)?.error?.toFriendlyMessage(),
        onSave = {
            submitted = true
            if (name.isNotBlank() && errors().all { it == null }) {
                val ac = Format.parseDecimal(acquisitionCost)!!
                val uses = Format.parseDecimal(estimatedUses)!!
                val maint = Format.parseDecimal(maintenance)!!
                val desc = description.ifBlank { null }
                if (id != null) {
                    viewModel.updateTool(id, ToolUpdateRequest(name.trim(), desc, ac, uses, maint, active))
                } else {
                    viewModel.createTool(ToolCreateRequest(name.trim(), desc, ac, uses, maint))
                }
            }
        }
    ) {
        SectionCard(title = "Herramienta") {
            AppTextField(name, { name = it }, "Nombre", error = if (submitted && name.isBlank()) "Obligatorio" else null)
            Spacer(Modifier.height(4.dp))
            AppTextField(description, { description = it }, "Descripción (opcional)", singleLine = false, minLines = 2)
        }
        SectionCard(title = "Desgaste") {
            NumberField(acquisitionCost, { acquisitionCost = it }, "Precio de compra", suffix = "€", error = shown[0])
            Spacer(Modifier.height(4.dp))
            NumberField(estimatedUses, { estimatedUses = it }, "Usos estimados", error = shown[1],
                helper = "Veces que se puede usar antes de reponerla")
            Spacer(Modifier.height(4.dp))
            NumberField(maintenance, { maintenance = it }, "Mantenimiento", suffix = "%", error = shown[2],
                helper = "Recargo sobre el coste por uso (limpieza, recambios…)")
        }
        if (id != null) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activa", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        }
    }
}
