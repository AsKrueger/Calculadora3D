package com.tdcostmanager.app.ui.common

import com.tdcostmanager.app.domain.util.NetworkError

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: NetworkError) : UiState<Nothing>
}
