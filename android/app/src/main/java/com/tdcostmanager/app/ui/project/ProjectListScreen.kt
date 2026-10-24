package com.tdcostmanager.app.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Long) -> Unit,
    onCreateProject: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.projectsState.collectAsState()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadProjects() }

    Scaffold(
        topBar = { AppTopBar("Proyectos") },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProject,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo proyecto") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading, UiState.Idle -> LoadingState()
                is UiState.Error -> ErrorState(s.error, onRetry = viewModel::loadProjects)
                is UiState.Success -> {
                    val (archived, active) = s.data.partition { it.status == ProjectStatus.ARCHIVED }
                    val visible = if (showArchived) archived else active
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.padding(horizontal = ScreenPadding, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !showArchived,
                                onClick = { showArchived = false },
                                label = { Text("Activos (${active.size})") }
                            )
                            FilterChip(
                                selected = showArchived,
                                onClick = { showArchived = true },
                                label = { Text("Archivados (${archived.size})") }
                            )
                        }
                        if (visible.isEmpty()) {
                            if (showArchived) {
                                EmptyState(Icons.Default.Inventory2, "Nada archivado", "Los proyectos que archives aparecerán aquí.")
                            } else {
                                EmptyState(
                                    icon = Icons.Default.ViewInAr,
                                    title = "Tu primer proyecto",
                                    message = "Crea un proyecto para cada encargo y calcula su presupuesto con los costes reales de tu taller.",
                                    actionLabel = "Crear proyecto",
                                    onAction = onCreateProject
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(visible, key = { it.id }) { project ->
                                    ProjectCard(project, onClick = { onProjectClick(project.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectResponse, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                project.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(12.dp))
            StatusChip(project.status.label, project.status.tone)
        }
        if (!project.description.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Text(
            project.summary(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** «1 material · 2 máquinas · 1,5 h de mano de obra» */
fun ProjectResponse.summary(): String {
    fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
    val parts = buildList {
        if (materials.isNotEmpty()) add(plural(materials.size, "material", "materiales"))
        if (machines.isNotEmpty()) add(plural(machines.size, "máquina", "máquinas"))
        if (tools.isNotEmpty()) add(plural(tools.size, "herramienta", "herramientas"))
        add("${Format.hours(laborHours)} de mano de obra")
    }
    return parts.joinToString(" · ")
}
