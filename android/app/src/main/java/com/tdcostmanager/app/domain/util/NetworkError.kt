package com.tdcostmanager.app.domain.util

import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkError {
    data object BadRequest : NetworkError
    data object NotFound : NetworkError
    data object Conflict : NetworkError
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object ServerError : NetworkError
    data object ConnectionError : NetworkError
    data class Unknown(val message: String?) : NetworkError
}

fun Throwable.toNetworkError(): NetworkError {
    return when (this) {
        is HttpException -> {
            when (code()) {
                400 -> NetworkError.BadRequest
                401 -> NetworkError.Unauthorized
                403 -> NetworkError.Forbidden
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

fun NetworkError.toFriendlyMessage(): String {
    return when (this) {
        is NetworkError.BadRequest -> "Los datos introducidos no son válidos."
        is NetworkError.Unauthorized -> "Correo o contraseña incorrectos."
        is NetworkError.Forbidden -> "No tienes permisos para realizar esta acción."
        is NetworkError.NotFound -> "No se ha encontrado el recurso solicitado."
        is NetworkError.Conflict -> "Este usuario ya existe."
        is NetworkError.ServerError -> "No se ha podido completar la operación. Inténtalo de nuevo más tarde."
        is NetworkError.ConnectionError -> "No se ha podido conectar con el servidor. Comprueba tu conexión a la red."
        is NetworkError.Unknown -> message ?: "Ha ocurrido un error inesperado."
    }
}
