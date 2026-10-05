package com.tdcostmanager.app.domain.util

import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkError {
    data object NotFound : NetworkError
    data object Conflict : NetworkError
    data object Unauthorized : NetworkError
    data object ServerError : NetworkError
    data object ConnectionError : NetworkError
    data class Unknown(val message: String?) : NetworkError
}

fun Throwable.toNetworkError(): NetworkError {
    return when (this) {
        is HttpException -> {
            when (code()) {
                401 -> NetworkError.Unauthorized
                404 -> NetworkError.NotFound
                409 -> NetworkError.Conflict
                in 500..599 -> NetworkError.ServerError
                else -> NetworkError.Unknown(message())
            }
        }
        is IOException -> NetworkError.ConnectionError
        else -> NetworkError.Unknown(localizedMessage)
    }
}
