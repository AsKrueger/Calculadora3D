package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.quote.QuoteCreateRequest
import com.tdcostmanager.app.data.remote.dto.quote.QuoteResponse
import retrofit2.http.*

interface QuoteApi {
    @POST("api/v1/projects/{projectId}/quotes")
    suspend fun createQuote(
        @Path("projectId") projectId: Long,
        @Body request: QuoteCreateRequest
    ): QuoteResponse

    @GET("api/v1/projects/{projectId}/quotes")
    suspend fun getQuotesByProject(
        @Path("projectId") projectId: Long
    ): List<QuoteResponse>

    @GET("api/v1/projects/{projectId}/quotes/{quoteId}")
    suspend fun getQuoteById(
        @Path("projectId") projectId: Long,
        @Path("quoteId") quoteId: Long
    ): QuoteResponse
}
