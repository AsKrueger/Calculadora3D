package com.tdcostmanager.app.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.data.repository.MachineRepository
import com.tdcostmanager.app.data.repository.MaterialRepository
import com.tdcostmanager.app.data.repository.ProjectRepository
import com.tdcostmanager.app.data.repository.ToolRepository
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Elementos del catálogo que se pueden añadir como líneas de un proyecto. */
data class ProjectCatalog(
    val materials: List<MaterialResponse>,
    val machines: List<MachineResponse>,
    val tools: List<ToolResponse>
)

class ProjectViewModel(
    private val repository: ProjectRepository,
    private val materialRepository: MaterialRepository,
    private val machineRepository: MachineRepository,
    private val toolRepository: ToolRepository
) : ViewModel() {

    private val _projectsState = MutableStateFlow<UiState<List<ProjectResponse>>>(UiState.Loading)
    val projectsState: StateFlow<UiState<List<ProjectResponse>>> = _projectsState.asStateFlow()

    private val _projectDetailState = MutableStateFlow<UiState<ProjectResponse>>(UiState.Idle)
    val projectDetailState: StateFlow<UiState<ProjectResponse>> = _projectDetailState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val operationState: StateFlow<UiState<Unit>> = _operationState.asStateFlow()

    private val _catalogState = MutableStateFlow<UiState<ProjectCatalog>>(UiState.Idle)
    val catalogState: StateFlow<UiState<ProjectCatalog>> = _catalogState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            // Al recargar una lista que ya se ve, se mantiene en pantalla en lugar de mostrar la carga
            if (_projectsState.value !is UiState.Success) _projectsState.value = UiState.Loading
            repository.getProjects()
                .onSuccess { list ->
                    _projectsState.value = UiState.Success(list)
                }
                .onFailure { e ->
                    _projectsState.value = UiState.Error(e.toNetworkError())
                }
        }
    }

    fun loadProjectDetail(id: Long) {
        viewModelScope.launch {
            _projectDetailState.value = UiState.Loading
            repository.getProject(id)
                .onSuccess { project ->
                    _projectDetailState.value = UiState.Success(project)
                }
                .onFailure { e ->
                    _projectDetailState.value = UiState.Error(e.toNetworkError())
                }
        }
    }

    /** Carga materiales, máquinas y herramientas en paralelo para el editor de líneas. */
    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = UiState.Loading
            val materials = async { materialRepository.getMaterials() }
            val machines = async { machineRepository.getMachines() }
            val tools = async { toolRepository.getTools() }
            val results = listOf(materials.await(), machines.await(), tools.await())
            val failure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
            _catalogState.value = if (failure != null) {
                UiState.Error(failure.toNetworkError())
            } else {
                UiState.Success(
                    ProjectCatalog(
                        materials = materials.await().getOrThrow(),
                        machines = machines.await().getOrThrow(),
                        tools = tools.await().getOrThrow()
                    )
                )
            }
        }
    }

    fun createProject(request: ProjectCreateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.createProject(request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadProjects()
                }
                .onFailure { e ->
                    _operationState.value = UiState.Error(e.toNetworkError())
                }
        }
    }

    fun updateProject(id: Long, request: ProjectUpdateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.updateProject(id, request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadProjectDetail(id)
                }
                .onFailure { e ->
                    _operationState.value = UiState.Error(e.toNetworkError())
                }
        }
    }

    fun archiveProject(id: Long) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.archiveProject(id)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadProjects()
                }
                .onFailure { e ->
                    _operationState.value = UiState.Error(e.toNetworkError())
                }
        }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}
