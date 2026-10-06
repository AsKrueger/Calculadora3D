package com.tdcostmanager.app.ui.machine

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.ui.common.UiState
import com.tdcostmanager.app.ui.material.ActiveBadge
import com.tdcostmanager.app.ui.material.InfoBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineDetailScreen(
    id: Long,
    viewModel: MachineViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.machineDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadMachineDetail(id)
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
                title = { Text("Detalle de Máquina") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state is UiState.Success) {
                        val machine = (state as UiState.Success).data
                        if (machine.active) {
                            IconButton(onClick = { onEditClick(machine.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.deactivateMachine(machine.id) }) {
                                Icon(Icons.Default.Archive, contentDescription = "Desactivar")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val machine = s.data
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = machine.name, style = MaterialTheme.typography.headlineMedium)
                        ActiveBadge(active = machine.active)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Adquisición", value = "${machine.acquisitionCost} €", modifier = Modifier.weight(1f))
                            InfoBox(label = "Potencia", value = "${machine.powerWatts} W", modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Vida Útil", value = "${machine.usefulLifeHours} h", modifier = Modifier.weight(1f))
                            InfoBox(label = "Mantenimiento", value = "${machine.maintenanceCostPerHour} €/h", modifier = Modifier.weight(1f))
                        }
                    }
                }
                is UiState.Error -> Text("Error", modifier = Modifier.align(Alignment.Center))
                else -> {}
            }
            if (operationState is UiState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
