package com.tdcostmanager.app.ui.machine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.ui.common.UiState
import com.tdcostmanager.app.ui.material.ActiveBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineListScreen(
    viewModel: MachineViewModel,
    onMachineClick: (Long) -> Unit,
    onCreateMachine: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.machinesState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Máquinas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMachine) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Máquina")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val machines = s.data
                    if (machines.isEmpty()) {
                        Text("No hay máquinas.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(machines) { machine ->
                                MachineItem(machine = machine, onClick = { onMachineClick(machine.id) })
                            }
                        }
                    }
                }
                is UiState.Error -> Button(onClick = { viewModel.loadMachines() }, modifier = Modifier.align(Alignment.Center)) { Text("Reintentar") }
                else -> {}
            }
        }
    }
}

@Composable
fun MachineItem(machine: MachineResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (machine.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = machine.name, style = MaterialTheme.typography.titleMedium)
                ActiveBadge(active = machine.active)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${machine.powerWatts} W",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
