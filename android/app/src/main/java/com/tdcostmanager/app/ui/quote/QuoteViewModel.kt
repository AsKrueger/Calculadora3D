package com.tdcostmanager.app.ui.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.data.remote.dto.quote.QuoteCreateRequest
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.data.repository.QuoteRepository
import com.tdcostmanager.app.domain.util.toNetworkError
import com.tdcostmanager.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuoteViewModel(
    private val repository: QuoteRepository
) : ViewModel() {

    private val _quotesState = MutableStateFlow<UiState<List<QuoteResponse>>>(UiState.Loading)
    val quotesState: StateFlow<UiState<List<QuoteResponse>>> = _quotesState.asStateFlow()

    private val _quoteDetailState = MutableStateFlow<UiState<QuoteResponse>>(UiState.Idle)
    val quoteDetailState: StateFlow<UiState<QuoteResponse>> = _quoteDetailState.asStateFlow()

    private val _createdQuoteState = MutableStateFlow<UiState<QuoteResponse>>(UiState.Idle)
    val createdQuoteState: StateFlow<UiState<QuoteResponse>> = _createdQuoteState.asStateFlow()

    fun loadQuotes(projectId: Long) {
        viewModelScope.launch {
            _quotesState.value = UiState.Loading
            repository.getQuotesByProject(projectId)
                .onSuccess { list -> _quotesState.value = UiState.Success(list) }
                .onFailure { e -> _quotesState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun loadQuoteDetail(projectId: Long, quoteId: Long) {
        viewModelScope.launch {
            _quoteDetailState.value = UiState.Loading
            repository.getQuoteById(projectId, quoteId)
                .onSuccess { quote -> _quoteDetailState.value = UiState.Success(quote) }
                .onFailure { e -> _quoteDetailState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun createQuote(projectId: Long, marginPercentage: Double, safetyPercentage: Double) {
        viewModelScope.launch {
            _createdQuoteState.value = UiState.Loading
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val request = QuoteCreateRequest(
                marginPercentage = marginPercentage,
                safetyPercentage = safetyPercentage,
                calculationDateTime = nowStr
            )
            repository.createQuote(projectId, request)
                .onSuccess { quote ->
                    _createdQuoteState.value = UiState.Success(quote)
                    loadQuotes(projectId)
                }
                .onFailure { e -> _createdQuoteState.value = UiState.Error(e.toNetworkError()) }
        }
    }

    fun resetCreatedQuoteState() {
        _createdQuoteState.value = UiState.Idle
    }
}
