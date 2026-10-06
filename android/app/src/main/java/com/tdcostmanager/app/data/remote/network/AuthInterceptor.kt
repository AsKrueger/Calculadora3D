package com.tdcostmanager.app.data.remote.network

import com.tdcostmanager.app.core.session.SessionEvent
import com.tdcostmanager.app.core.session.SessionEventBus
import com.tdcostmanager.app.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // Si existe un token, lo inyectamos en la cabecera
        tokenManager.getToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Si recibimos un 401 y NO es una petición a los endpoints de auth
        if (response.code == 401 && !request.url.encodedPath.contains("/api/v1/auth/")) {
            tokenManager.clearToken()
            runBlocking {
                SessionEventBus.emit(SessionEvent.Logout)
            }
        }

        return response
    }
}
