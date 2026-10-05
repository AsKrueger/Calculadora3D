package com.tdcostmanager.app.ui.material

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailScreen(
    id: Long,
    viewModel: MaterialViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit
) {
    val state by viewModel.materialDetailState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    LaunchedEffect(id) {
        viewModel.loadMaterialDetail(id)
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
                title = { Text("Detalle de Material") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state is UiState.Success) {
                        val material = (state as UiState.Success).data
                        if (material.active) {
                            IconButton(onClick = { onEditClick(material.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(onClick = { viewModel.deactivateMaterial(material.id) }) {
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
                    val material = s.data
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(text = material.name, style = MaterialTheme.typography.headlineMedium)
                        ActiveBadge(active = material.active)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = "Descripción", style = MaterialTheme.typography.titleSmall)
                        Text(text = material.description ?: "Sin descripción", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoBox(label = "Precio", value = "${material.purchasePrice} €", modifier = Modifier.weight(1f))
                            InfoBox(label = "Cantidad", value = "${material.quantity} ${material.unit}", modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Categoría: ${material.category}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is UiState.Error -> Text("Error", modifier = Modifier.align(Alignment.Center))
                else -> {}
            }
            if (operationState is UiState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}
