package com.tdcostmanager.app.ui.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialListScreen(
    viewModel: MaterialViewModel,
    onMaterialClick: (Long) -> Unit,
    onCreateMaterial: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.materialsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Materiales") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMaterial) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Material")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val materials = s.data
                    if (materials.isEmpty()) {
                        Text("No hay materiales.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(materials) { material ->
                                MaterialItem(material = material, onClick = { onMaterialClick(material.id) })
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error al cargar materiales")
                        Button(onClick = { viewModel.loadMaterials() }) { Text("Reintentar") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun MaterialItem(material: MaterialResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (material.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = material.name, style = MaterialTheme.typography.titleMedium)
                ActiveBadge(active = material.active)
            }
            Text(
                text = "${material.purchasePrice} € / ${material.quantity} ${material.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActiveBadge(active: Boolean) {
    val text = if (active) "ACTIVO" else "INACTIVO"
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        color = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
