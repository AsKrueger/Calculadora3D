package com.tdcostmanager.app.data.repository

import com.tdcostmanager.app.data.local.TokenManager
import com.tdcostmanager.app.data.remote.api.AuthApi
import com.tdcostmanager.app.data.remote.dto.auth.LoginRequest
import com.tdcostmanager.app.data.remote.dto.auth.RegisterRequest
import com.tdcostmanager.app.data.remote.network.NetworkConfig

class AuthRepository(
    private val api: AuthApi = NetworkConfig.authApi,
    private val tokenManager: TokenManager
) {
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            api.register(RegisterRequest(email, password))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveToken(response.token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    fun isAuthenticated(): Boolean {
        return tokenManager.getToken() != null
    }
}
