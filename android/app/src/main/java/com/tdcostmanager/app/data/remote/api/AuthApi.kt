package com.tdcostmanager.app.data.remote.api

import com.tdcostmanager.app.data.remote.dto.auth.AuthResponse
import com.tdcostmanager.app.data.remote.dto.auth.LoginRequest
import com.tdcostmanager.app.data.remote.dto.auth.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest)

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
