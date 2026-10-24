package com.tdcostmanager.app.ui.machine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.machine.MachineCreateRequest
import com.tdcostmanager.app.data.remote.dto.machine.MachineUpdateRequest
import com.tdcostmanager.app.domain.util.toFriendlyMessage
import com.tdcostmanager.app.ui.common.*

@Composable
fun MachineFormScreen(
    id: Long?,
    viewModel: MachineViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.machineDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var acquisitionCost by rememberSaveable { mutableStateOf("") }
    var usefulLifeHours by rememberSaveable { mutableStateOf("") }
    var powerWatts by rememberSaveable { mutableStateOf("") }
    var maintenance by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) { if (id != null) viewModel.loadMachineDetail(id) }
    LaunchedEffect(detailState) {
        val s = detailState
        if (id != null && !loaded && s is UiState.Success) {
            val m = s.data
            name = m.name
            acquisitionCost = Format.forInput(m.acquisitionCost)
            usefulLifeHours = Format.forInput(m.usefulLifeHours)
            powerWatts = Format.forInput(m.powerWatts)
            maintenance = Format.forInput(m.maintenanceCostPerHour)
            active = m.active
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
        validateNumber(usefulLifeHours, positive = true),
        validateNumber(powerWatts),
        validateNumber(maintenance)
    )
    val shown = if (submitted) errors() else List(4) { null }

    FormScaffold(
        title = if (id != null) "Editar máquina" else "Nueva máquina",
        onBack = onNavigateBack,
        saveLabel = "Guardar máquina",
        saving = operationState is UiState.Loading,
        error = (operationState as? UiState.Error)?.error?.toFriendlyMessage(),
        onSave = {
            submitted = true
            if (name.isNotBlank() && errors().all { it == null }) {
                val ac = Format.parseDecimal(acquisitionCost)!!
                val life = Format.parseDecimal(usefulLifeHours)!!
                val watts = Format.parseDecimal(powerWatts)!!
                val maint = Format.parseDecimal(maintenance)!!
                if (id != null) {
                    viewModel.updateMachine(id, MachineUpdateRequest(name.trim(), ac, life, watts, maint, active))
                } else {
                    viewModel.createMachine(MachineCreateRequest(name.trim(), ac, life, watts, maint))
                }
            }
        }
    ) {
        SectionCard(title = "Máquina") {
            AppTextField(name, { name = it }, "Nombre", error = if (submitted && name.isBlank()) "Obligatorio" else null)
            Spacer(Modifier.height(4.dp))
            NumberField(powerWatts, { powerWatts = it }, "Potencia media", suffix = "W", error = shown[2],
                helper = "Consumo medio mientras imprime")
        }
        SectionCard(title = "Amortización") {
            NumberField(acquisitionCost, { acquisitionCost = it }, "Precio de compra", suffix = "€", error = shown[0])
            Spacer(Modifier.height(4.dp))
            NumberField(usefulLifeHours, { usefulLifeHours = it }, "Vida útil estimada", suffix = "h", error = shown[1],
                helper = "Horas de impresión hasta reponerla")
            Spacer(Modifier.height(4.dp))
            NumberField(maintenance, { maintenance = it }, "Mantenimiento", suffix = "€/h", error = shown[3],
                helper = "Boquillas, correas, lubricación… repartido por hora")
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
