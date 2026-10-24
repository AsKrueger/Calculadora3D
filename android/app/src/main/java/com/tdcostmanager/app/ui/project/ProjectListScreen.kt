package com.tdcostmanager.app.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.ui.common.*
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Long) -> Unit,
    onCreateProject: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.projectsState.collectAsState()
    var filter by rememberSaveable { mutableStateOf(ProjectFilter.Active) }
    var sort by rememberSaveable { mutableStateOf(ProjectSort.Recent) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSortSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadProjects() }

    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = { GlowFab("Nuevo proyecto", Icons.Default.Add, onCreateProject) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Header(
                searching = searching,
                sortActive = sort != ProjectSort.Recent,
                onSearch = {
                    searching = !searching
                    if (!searching) query = ""
                },
                onSort = { showSortSheet = true }
            )
            AnimatedVisibility(visible = searching) {
                SearchField(query, onQueryChange = { query = it }, onClose = { searching = false; query = "" })
            }

            when (val s = state) {
                is UiState.Loading, UiState.Idle -> LoadingState()
                is UiState.Error -> ErrorState(s.error, onRetry = viewModel::loadProjects)
                is UiState.Success -> {
                    val all = s.data
                    Row(
                        Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PillChip("Activos", filter == ProjectFilter.Active, { filter = ProjectFilter.Active }, count = all.count { it.matches(ProjectFilter.Active) })
                        PillChip("Archivados", filter == ProjectFilter.Archived, { filter = ProjectFilter.Archived }, count = all.count { it.matches(ProjectFilter.Archived) })
                        PillChip("Todos", filter == ProjectFilter.All, { filter = ProjectFilter.All }, outlined = false)
                    }
                    val visible = all.query(filter, query, sort)
                    when {
                        visible.isNotEmpty() -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(visible, key = { it.id }) { project ->
                                ProjectCard(project, onClick = { onProjectClick(project.id) })
                            }
                            item {
                                Text(
                                    "Precio del último presupuesto de cada proyecto, calculado con materiales, amortización, electricidad y mano de obra.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                        query.isNotBlank() -> EmptyState(Icons.Default.SearchOff, "Sin resultados", "Ningún proyecto contiene «${query.trim()}».")
                        filter == ProjectFilter.Archived -> EmptyState(Icons.Default.Inventory2, "Nada archivado", "Los proyectos que archives aparecerán aquí.")
                        else -> EmptyState(
                            icon = Icons.Default.ViewInAr,
                            title = "Tu primer proyecto",
                            message = "Crea un proyecto para cada encargo y calcula su presupuesto con los costes reales de tu taller.",
                            actionLabel = "Crear proyecto",
                            onAction = onCreateProject
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 32.dp)) {
                Text("Ordenar proyectos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
                ProjectSort.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { sort = option; showSortSheet = false }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sort == option, onClick = { sort = option; showSortSheet = false })
                        Spacer(Modifier.width(8.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(searching: Boolean, sortActive: Boolean, onSearch: () -> Unit, onSort: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding + 4.dp, end = ScreenPadding, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Proyectos", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold))
            Text("3D Cost & Production Manager", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        }
        CircleIconButton(Icons.Outlined.Search, "Buscar", onSearch, selected = searching)
        Spacer(Modifier.width(14.dp))
        CircleIconButton(Icons.Outlined.Tune, "Ordenar", onSort, selected = sortActive)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val field = AppThemeExtras.field
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar por nombre o descripción", color = MaterialTheme.colorScheme.outline) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda") } },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = field.container,
            focusedContainerColor = field.container,
            unfocusedBorderColor = field.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 4.dp)
            .focusRequester(focus)
    )
}

/** Color de la etiqueta de precio según el estado del proyecto. */
private val ProjectStatus.priceTone: Tone
    get() = when (this) {
        ProjectStatus.DRAFT -> Tone.Warning
        ProjectStatus.IN_PROGRESS -> Tone.Progress
        ProjectStatus.COMPLETED -> Tone.Success
        ProjectStatus.CANCELLED -> Tone.Danger
        ProjectStatus.ARCHIVED -> Tone.Neutral
    }

@Composable
private fun ProjectCard(project: ProjectResponse, onClick: () -> Unit) {
    ElevatedSectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                project.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(12.dp))
            StatusChip(project.status.label, project.status.tone)
        }
        if (!project.description.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineSummary(project, Modifier.weight(1f))
            // Sin etiqueta de precio: el proyecto aún no tiene presupuestos
            project.latestQuote?.let {
                Spacer(Modifier.width(8.dp))
                PriceTag(Format.euros(it.finalPrice), project.status.priceTone)
            }
        }
    }
}

@Composable
private fun LineSummary(project: ProjectResponse, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val style = MaterialTheme.typography.bodyMedium
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val parts = project.lineParts()
        if (parts.isNotEmpty()) {
            Icon(Icons.Outlined.ViewInAr, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                parts.joinToString("  •  "),
                style = style,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text("  •  ", style = style, color = muted.copy(alpha = 0.5f))
        }
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(Format.hours(project.laborHours), style = style, color = muted, maxLines = 1)
    }
}
