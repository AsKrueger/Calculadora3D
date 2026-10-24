package com.tdcostmanager.app.ui.machine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular

/** Amortización más mantenimiento por hora de uso, sin contar la electricidad. */
val MachineResponse.hourlyCost: Double
    get() = (if (usefulLifeHours > 0) acquisitionCost / usefulLifeHours else 0.0) + maintenanceCostPerHour

@Composable
fun MachineListContent(viewModel: MachineViewModel, onMachineClick: (Long) -> Unit) {
    val state by viewModel.machinesState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMachines() }

    when (val s = state) {
        is UiState.Loading, UiState.Idle -> LoadingState()
        is UiState.Error -> ErrorState(s.error, onRetry = viewModel::loadMachines)
        is UiState.Success -> if (s.data.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Print,
                title = "Sin máquinas",
                message = "Añade tus impresoras para repartir su coste y su consumo entre las horas de impresión."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.data, key = { it.id }) { machine ->
                    ListRowCard(
                        title = machine.name,
                        subtitle = "${Format.number(machine.powerWatts)} W · ${Format.number(machine.usefulLifeHours, 0)} h de vida útil",
                        icon = Icons.Default.Print,
                        dimmed = !machine.active,
                        onClick = { onMachineClick(machine.id) },
                        trailing = {
                            if (machine.active) {
                                Text(
                                    "${Format.number(machine.hourlyCost, 3)} €/h",
                                    style = MaterialTheme.typography.labelLarge.tabular(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                StatusChip("Inactiva", Tone.Neutral)
                            }
                        }
                    )
                }
            }
        }
    }
}
