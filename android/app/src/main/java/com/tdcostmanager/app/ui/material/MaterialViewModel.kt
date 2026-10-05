package com.tdcostmanager.app.ui.material

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.material.MaterialCreateRequest
import com.tdcostmanager.app.data.remote.dto.material.MaterialResponse
import com.tdcostmanager.app.data.remote.dto.material.MaterialUpdateRequest
import com.tdcostmanager.app.data.repository.MaterialRepository
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialViewModel(
    private val repository: MaterialRepository
) : ViewModel() {

    private val _materialsState = MutableStateFlow<UiState<List<MaterialResponse>>>(UiState.Loading)
    val materialsState: StateFlow<UiState<List<MaterialResponse>>> = _materialsState.asStateFlow()

    private val _materialDetailState = MutableStateFlow<UiState<MaterialResponse>>(UiState.Idle)
    val materialDetailState: StateFlow<UiState<MaterialResponse>> = _materialDetailState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val operationState: StateFlow<UiState<Unit>> = _operationState.asStateFlow()

    init {
        loadMaterials()
    }

    fun loadMaterials() {
        viewModelScope.launch {
            _materialsState.value = UiState.Loading
            repository.getMaterials()
                .onSuccess { list -> _materialsState.value = UiState.Success(list) }
                .onFailure { e -> _materialsState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun loadMaterialDetail(id: Long) {
        viewModelScope.launch {
            _materialDetailState.value = UiState.Loading
            repository.getMaterial(id)
                .onSuccess { material -> _materialDetailState.value = UiState.Success(material) }
                .onFailure { e -> _materialDetailState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun createMaterial(request: MaterialCreateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.createMaterial(request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMaterials()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun updateMaterial(id: Long, request: MaterialUpdateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.updateMaterial(id, request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMaterialDetail(id)
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun deactivateMaterial(id: Long) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.deactivateMaterial(id)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMaterials()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}
