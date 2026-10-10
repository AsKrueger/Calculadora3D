package com.tdcostmanager.app.ui.material

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
import com.tdcostmanager.app.data.remote.dto.material.MaterialCreateRequest
import com.tdcostmanager.app.data.remote.dto.material.MaterialUpdateRequest
import com.tdcostmanager.app.domain.model.MaterialCategory
import com.tdcostmanager.app.domain.model.UnitType
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialFormScreen(
    id: Long?,
    viewModel: MaterialViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.materialDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(UnitType.KG) }
    var category by remember { mutableStateOf(MaterialCategory.FILAMENT) }
    var active by remember { mutableStateOf(true) }

    val isEdit = id != null

    LaunchedEffect(id) {
        if (id != null) viewModel.loadMaterialDetail(id)
    }

    LaunchedEffect(detailState) {
        if (isEdit && detailState is UiState.Success) {
            val material = (detailState as UiState.Success).data
            name = material.name
            description = material.description ?: ""
            price = material.purchasePrice.toString()
            quantity = material.quantity.toString()
            unit = material.unit
            category = material.category
            active = material.active
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
                title = { Text(if (isEdit) "Editar Material" else "Nuevo Material") },
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio (€)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Unidad:", modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UnitType.values().forEach { u ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { unit = u },
                        label = { Text(u.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Categoría:", modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialCategory.values().take(3).forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.name) })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val q = quantity.toDoubleOrNull() ?: 0.0
                    if (id != null) {
                        viewModel.updateMaterial(id, MaterialUpdateRequest(name, description, p, q, unit, category, active))
                    } else {
                        viewModel.createMaterial(MaterialCreateRequest(name, description, p, q, unit, category))
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
