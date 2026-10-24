package com.tdcostmanager.app.ui.tool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular

/** Coste de cada uso, con el porcentaje de mantenimiento incluido. */
val ToolResponse.costPerUse: Double
    get() = if (estimatedUses > 0) acquisitionCost / estimatedUses * (1 + maintenancePercentage / 100) else 0.0

@Composable
fun ToolListContent(viewModel: ToolViewModel, onToolClick: (Long) -> Unit) {
    val state by viewModel.toolsState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadTools() }

    when (val s = state) {
        is UiState.Loading, UiState.Idle -> LoadingState()
        is UiState.Error -> ErrorState(s.error, onRetry = viewModel::loadTools)
        is UiState.Success -> if (s.data.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Handyman,
                title = "Sin herramientas",
                message = "Añade lijas, aerógrafos o cualquier útil que se desgaste con el uso para repartir su coste."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.data, key = { it.id }) { tool ->
                    ListRowCard(
                        title = tool.name,
                        subtitle = "${Format.number(tool.estimatedUses, 0)} usos · ${Format.euros(tool.acquisitionCost)}",
                        icon = Icons.Default.Handyman,
                        dimmed = !tool.active,
                        onClick = { onToolClick(tool.id) },
                        trailing = {
                            if (tool.active) {
                                Text(
                                    "${Format.number(tool.costPerUse, 3)} €/uso",
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
