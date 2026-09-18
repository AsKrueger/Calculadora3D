package com.tdcostmanager.app.presentation.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HealthUiState {
    object Loading : HealthUiState
    data class Success(val status: String) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

class HealthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState

    init {
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _uiState.value = HealthUiState.Loading
            try {
                val response = RetrofitClient.healthApi.getHealth()
                _uiState.value = HealthUiState.Success(response.status)
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error(e.localizedMessage ?: "Error de conexión")
            }
        }
    }
}
