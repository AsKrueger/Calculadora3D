package com.tdcostmanager.app.ui.tool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.tool.ToolCreateRequest
import com.tdcostmanager.app.data.remote.dto.tool.ToolResponse
import com.tdcostmanager.app.data.remote.dto.tool.ToolUpdateRequest
import com.tdcostmanager.app.data.repository.ToolRepository
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToolViewModel(
    private val repository: ToolRepository
) : ViewModel() {

    private val _toolsState = MutableStateFlow<UiState<List<ToolResponse>>>(UiState.Loading)
    val toolsState: StateFlow<UiState<List<ToolResponse>>> = _toolsState.asStateFlow()

    private val _toolDetailState = MutableStateFlow<UiState<ToolResponse>>(UiState.Idle)
    val toolDetailState: StateFlow<UiState<ToolResponse>> = _toolDetailState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val operationState: StateFlow<UiState<Unit>> = _operationState.asStateFlow()

    init {
        loadTools()
    }

    fun loadTools() {
        viewModelScope.launch {
            _toolsState.value = UiState.Loading
            repository.getTools()
                .onSuccess { list -> _toolsState.value = UiState.Success(list) }
                .onFailure { e -> _toolsState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun loadToolDetail(id: Long) {
        viewModelScope.launch {
            _toolDetailState.value = UiState.Loading
            repository.getTool(id)
                .onSuccess { tool -> _toolDetailState.value = UiState.Success(tool) }
                .onFailure { e -> _toolDetailState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun createTool(request: ToolCreateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.createTool(request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadTools()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun updateTool(id: Long, request: ToolUpdateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.updateTool(id, request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadToolDetail(id)
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun deactivateTool(id: Long) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.deactivateTool(id)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadTools()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}
