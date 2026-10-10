package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormScreen(
    id: Long?,
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.projectDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var laborHours by remember { mutableStateOf("") }
    var laborCostPerHour by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ProjectStatus.DRAFT) }

    val isEdit = id != null

    LaunchedEffect(id) {
        if (id != null) {
            viewModel.loadProjectDetail(id)
        }
    }

    LaunchedEffect(detailState) {
        if (isEdit && detailState is UiState.Success) {
            val project = (detailState as UiState.Success).data
            name = project.name
            description = project.description ?: ""
            laborHours = project.laborHours.toString()
            laborCostPerHour = project.laborCostPerHour.toString()
            status = project.status
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
                title = { Text(if (isEdit) "Editar Proyecto" else "Nuevo Proyecto") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = laborHours,
                    onValueChange = { laborHours = it },
                    label = { Text("Horas") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = laborCostPerHour,
                    onValueChange = { laborCostPerHour = it },
                    label = { Text("Coste/h") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (operationState is UiState.Error) {
                Text(
                    text = (operationState as UiState.Error).error.toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    val hours = laborHours.toDoubleOrNull() ?: 0.0
                    val cost = laborCostPerHour.toDoubleOrNull() ?: 0.0
                    if (id != null) {
                        viewModel.updateProject(id, ProjectUpdateRequest(name, description, status, hours, cost))
                    } else {
                        viewModel.createProject(name, description, hours, cost)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && operationState !is UiState.Loading
            ) {
                if (operationState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}
