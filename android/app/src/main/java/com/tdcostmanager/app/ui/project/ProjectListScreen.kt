package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Long) -> Unit,
    onCreateProject: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToMachines: () -> Unit,
    onNavigateToTools: () -> Unit
) {
    val projectsState by viewModel.projectsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proyectos") },
                actions = {
                    TextButton(onClick = onNavigateToMaterials) { Text("Materiales") }
                    TextButton(onClick = onNavigateToMachines) { Text("Máquinas") }
                    TextButton(onClick = onNavigateToTools) { Text("Herramientas") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Proyecto")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = projectsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Success -> {
                    val projects = state.data
                    if (projects.isEmpty()) {
                        Text(
                            text = "No hay proyectos disponibles.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(projects) { project ->
                                ProjectItem(project = project, onClick = { onProjectClick(project.id) })
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error al cargar proyectos")
                        Button(onClick = { viewModel.loadProjects() }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProjectItem(project: ProjectResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (project.status == ProjectStatus.ARCHIVED) 
                MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                StatusBadge(status = project.status)
            }
            project.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: ProjectStatus) {
    val color = when (status) {
        ProjectStatus.DRAFT -> MaterialTheme.colorScheme.secondary
        ProjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        ProjectStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        ProjectStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
        ProjectStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
