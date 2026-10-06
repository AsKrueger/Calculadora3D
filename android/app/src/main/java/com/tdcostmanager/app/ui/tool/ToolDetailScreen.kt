package com.tdcostmanager.app.ui.tool

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
fun ToolDetailScreen(
    id: Long,
    viewModel: ToolViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.toolDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadToolDetail(id)
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
                title = { Text("Detalle de Herramienta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state is UiState.Success) {
                        val tool = (state as UiState.Success).data
                        if (tool.active) {
                            IconButton(onClick = { onEditClick(tool.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.deactivateTool(tool.id) }) {
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
                    val tool = s.data
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = tool.name, style = MaterialTheme.typography.headlineMedium)
                        ActiveBadge(active = tool.active)
                        tool.description?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Adquisición", value = "${tool.acquisitionCost} €", modifier = Modifier.weight(1f))
                            InfoBox(label = "Usos Estimados", value = "${tool.estimatedUses}", modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Mantenimiento", value = "${tool.maintenancePercentage}%", modifier = Modifier.weight(1f))
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
