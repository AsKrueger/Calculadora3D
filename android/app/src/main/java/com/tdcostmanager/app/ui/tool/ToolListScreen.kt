package com.tdcostmanager.app.ui.tool

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
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.ui.common.UiState
import com.tdcostmanager.app.ui.material.ActiveBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolListScreen(
    viewModel: ToolViewModel,
    onToolClick: (Long) -> Unit,
    onCreateTool: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.toolsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herramientas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTool) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Herramienta")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val tools = s.data
                    if (tools.isEmpty()) {
                        Text("No hay herramientas.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tools) { tool ->
                                ToolItem(tool = tool, onClick = { onToolClick(tool.id) })
                            }
                        }
                    }
                }
                is UiState.Error -> Button(onClick = { viewModel.loadTools() }, modifier = Modifier.align(Alignment.Center)) { Text("Reintentar") }
                else -> {}
            }
        }
    }
}

@Composable
fun ToolItem(tool: ToolResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (tool.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = tool.name, style = MaterialTheme.typography.titleMedium)
                ActiveBadge(active = tool.active)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Usos estimados: ${tool.estimatedUses} | Mantenimiento: ${tool.maintenancePercentage}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
