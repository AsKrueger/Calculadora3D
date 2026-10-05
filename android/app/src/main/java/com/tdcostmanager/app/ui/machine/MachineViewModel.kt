package com.tdcostmanager.app.ui.machine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.machine.MachineCreateRequest
import com.tdcostmanager.app.data.remote.dto.machine.MachineResponse
import com.tdcostmanager.app.data.remote.dto.machine.MachineUpdateRequest
import com.tdcostmanager.app.data.repository.MachineRepository
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MachineViewModel(
    private val repository: MachineRepository
) : ViewModel() {

    private val _machinesState = MutableStateFlow<UiState<List<MachineResponse>>>(UiState.Loading)
    val machinesState: StateFlow<UiState<List<MachineResponse>>> = _machinesState.asStateFlow()

    private val _machineDetailState = MutableStateFlow<UiState<MachineResponse>>(UiState.Idle)
    val machineDetailState: StateFlow<UiState<MachineResponse>> = _machineDetailState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val operationState: StateFlow<UiState<Unit>> = _operationState.asStateFlow()

    init {
        loadMachines()
    }

    fun loadMachines() {
        viewModelScope.launch {
            _machinesState.value = UiState.Loading
            repository.getMachines()
                .onSuccess { list -> _machinesState.value = UiState.Success(list) }
                .onFailure { e -> _machinesState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun loadMachineDetail(id: Long) {
        viewModelScope.launch {
            _machineDetailState.value = UiState.Loading
            repository.getMachine(id)
                .onSuccess { machine -> _machineDetailState.value = UiState.Success(machine) }
                .onFailure { e -> _machineDetailState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun createMachine(request: MachineCreateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.createMachine(request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMachines()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun updateMachine(id: Long, request: MachineUpdateRequest) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.updateMachine(id, request)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMachineDetail(id)
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun deactivateMachine(id: Long) {
        viewModelScope.launch {
            _operationState.value = UiState.Loading
            repository.deactivateMachine(id)
                .onSuccess {
                    _operationState.value = UiState.Success(Unit)
                    loadMachines()
                }
                .onFailure { e -> _operationState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}
