package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.remote.api.QuoteApi
import com.tdcostmanager.app.data.remote.dto.quote.QuoteCreateRequest
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class QuoteRepository(
    private val api: QuoteApi = NetworkConfig.quoteApi
) {
    suspend fun createQuote(projectId: Long, request: QuoteCreateRequest): Result<QuoteResponse> {
        return try {
            Result.success(api.createQuote(projectId, request))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuotesByProject(projectId: Long): Result<List<QuoteResponse>> {
        return try {
            Result.success(api.getQuotesByProject(projectId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuoteById(projectId: Long, quoteId: Long): Result<QuoteResponse> {
        return try {
            Result.success(api.getQuoteById(projectId, quoteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
