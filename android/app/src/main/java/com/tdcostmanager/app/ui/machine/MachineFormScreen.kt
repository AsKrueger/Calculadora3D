package com.tdcostmanager.app.ui.machine

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
import com.tdcostmanager.app.data.remote.dto.machine.MachineCreateRequest
import com.tdcostmanager.app.data.remote.dto.machine.MachineUpdateRequest
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineFormScreen(
    id: Long?,
    viewModel: MachineViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.machineDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var acquisitionCost by remember { mutableStateOf("") }
    var usefulLifeHours by remember { mutableStateOf("") }
    var powerWatts by remember { mutableStateOf("") }
    var maintenanceCostPerHour by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    val isEdit = id != null

    LaunchedEffect(id) {
        if (id != null) viewModel.loadMachineDetail(id)
    }

    LaunchedEffect(detailState) {
        if (isEdit && detailState is UiState.Success) {
            val machine = (detailState as UiState.Success).data
            name = machine.name
            acquisitionCost = machine.acquisitionCost.toString()
            usefulLifeHours = machine.usefulLifeHours.toString()
            powerWatts = machine.powerWatts.toString()
            maintenanceCostPerHour = machine.maintenanceCostPerHour.toString()
            active = machine.active
        }
    }

    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            viewModel.resetOperationState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar Máquina" else "Nueva Máquina") },
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = acquisitionCost,
                    onValueChange = { acquisitionCost = it },
                    label = { Text("Coste Adquisición (€)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = powerWatts,
                    onValueChange = { powerWatts = it },
                    label = { Text("Potencia (W)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = usefulLifeHours,
                    onValueChange = { usefulLifeHours = it },
                    label = { Text("Vida Útil (h)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maintenanceCostPerHour,
                    onValueChange = { maintenanceCostPerHour = it },
                    label = { Text("Mantenimiento (€/h)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val ac = acquisitionCost.toDoubleOrNull() ?: 0.0
                    val ulh = usefulLifeHours.toDoubleOrNull() ?: 0.0
                    val pw = powerWatts.toDoubleOrNull() ?: 0.0
                    val mcph = maintenanceCostPerHour.toDoubleOrNull() ?: 0.0
                    
                    if (id != null) {
                        viewModel.updateMachine(id, MachineUpdateRequest(name, ac, ulh, pw, mcph, active))
                    } else {
                        viewModel.createMachine(MachineCreateRequest(name, ac, ulh, pw, mcph))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && operationState !is UiState.Loading
            ) {
                if (operationState is UiState.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Guardar")
            }
        }
    }
}
