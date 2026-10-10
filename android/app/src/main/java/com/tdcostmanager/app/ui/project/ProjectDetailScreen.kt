package com.tdcostmanager.app.ui.project

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
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    id: Long,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onNavigateToQuotes: (Long) -> Unit,
    onNavigateToCalculator: (Long) -> Unit
) {
    val detailState by viewModel.projectDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadProjectDetail(id)
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
                title = { Text("Detalle de Proyecto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (detailState is UiState.Success) {
                        val project = (detailState as UiState.Success).data
                        if (project.status != ProjectStatus.ARCHIVED) {
                            IconButton(onClick = { onEditClick(project.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.archiveProject(project.id) }) {
                                Icon(Icons.Default.Archive, contentDescription = "Archivar")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = detailState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Success -> {
                    val project = state.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(text = project.name, style = MaterialTheme.typography.headlineMedium)
                        StatusBadge(status = project.status)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(text = "Descripción", style = MaterialTheme.typography.titleSmall)
                        Text(text = project.description ?: "Sin descripción", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoItem(label = "Horas Laborales", value = "${project.laborHours} h", modifier = Modifier.weight(1f))
                            InfoItem(label = "Coste/Hora", value = "${project.laborCostPerHour} €", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onNavigateToCalculator(project.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Calcular Costes y Generar Presupuesto")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onNavigateToQuotes(project.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Presupuestos Emitidos")
                        }
                    }
                }
                is UiState.Error -> {
                    Text(text = "Error al cargar el proyecto", modifier = Modifier.align(Alignment.Center))
                }
                else -> {}
            }
            
            if (operationState is UiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}
