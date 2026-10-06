package com.tdcostmanager.app.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.project.ProjectCreateRequest
import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.data.remote.dto.project.ProjectUpdateRequest
import com.tdcostmanager.app.data.repository.ProjectRepository
import com.tdcostmanager.app.domain.util.NetworkError
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _projectsState = MutableStateFlow<UiState<List<ProjectResponse>>>(UiState.Loading)
    val projectsState: StateFlow<UiState<List<ProjectResponse>>> = _projectsState.asStateFlow()

    private val _projectDetailState = MutableStateFlow<UiState<ProjectResponse>>(UiState.Idle)
    val projectDetailState: StateFlow<UiState<ProjectResponse>> = _projectDetailState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val operationState: StateFlow<UiState<Unit>> = _operationState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = UiState.Loading
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

    fun createProject(name: String, description: String?, laborHours: Double, laborCostPerHour: Double) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            val request = ProjectCreateRequest(name, description, laborHours, laborCostPerHour)
            repository.createProject(request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadProjects() // Refresh list
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
                    loadProjectDetail(id) // Refresh detail
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
                    loadProjects() // Refresh list
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
