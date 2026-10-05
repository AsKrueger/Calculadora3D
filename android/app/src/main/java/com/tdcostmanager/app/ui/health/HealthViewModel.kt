package com.tdcostmanager.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data class Success(val status: String) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

class HealthViewModel(
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _uiState.value = HealthUiState.Loading
            repository.getBackendHealth()
                .onSuccess { response ->
                    _uiState.value = HealthUiState.Success(response.status)
                }
                .onFailure { error ->
                    _uiState.value = HealthUiState.Error(
                        error.localizedMessage ?: "Fallo de conexión con el servidor"
                    )
                }
        }
    }
}
