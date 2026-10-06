package com.tdcostmanager.app.ui.tool

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
import com.tdcostmanager.app.data.remote.dto.tool.ToolCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolUpdateRequest
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolFormScreen(
    id: Long?,
    viewModel: ToolViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.toolDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var acquisitionCost by remember { mutableStateOf("") }
    var estimatedUses by remember { mutableStateOf("") }
    var maintenancePercentage by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    val isEdit = id != null

    LaunchedEffect(id) {
        if (isEdit && id != null) viewModel.loadToolDetail(id)
    }

    LaunchedEffect(detailState) {
        if (isEdit && detailState is UiState.Success) {
            val tool = (detailState as UiState.Success).data
            name = tool.name
            description = tool.description ?: ""
            acquisitionCost = tool.acquisitionCost.toString()
            estimatedUses = tool.estimatedUses.toString()
            maintenancePercentage = tool.maintenancePercentage.toString()
            active = tool.active
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
                title = { Text(if (isEdit) "Editar Herramienta" else "Nueva Herramienta") },
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

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
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
                    value = estimatedUses,
                    onValueChange = { estimatedUses = it },
                    label = { Text("Usos Estimados") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = maintenancePercentage,
                onValueChange = { maintenancePercentage = it },
                label = { Text("Mantenimiento (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val ac = acquisitionCost.toDoubleOrNull() ?: 0.0
                    val eu = estimatedUses.toDoubleOrNull() ?: 1.0
                    val mp = maintenancePercentage.toDoubleOrNull() ?: 0.0
                    val desc = description.ifBlank { null }
                    
                    if (isEdit && id != null) {
                        viewModel.updateTool(id, ToolUpdateRequest(name, desc, ac, eu, mp, active))
                    } else {
                        viewModel.createTool(ToolCreateRequest(name, desc, ac, eu, mp))
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
