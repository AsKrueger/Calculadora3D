package com.tdcostmanager.app.domain.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

sealed interface NetworkError {
    // En 400 y 409 el backend explica el motivo en ApiError.message; se conserva para mostrarlo
    data class BadRequest(val serverMessage: String? = null) : NetworkError
    data object NotFound : NetworkError
    data class Conflict(val serverMessage: String? = null) : NetworkError
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object ServerError : NetworkError
    data object ConnectionError : NetworkError
    data class Unknown(val message: String?) : NetworkError
}

@Serializable
private data class ApiErrorBody(val message: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

private fun HttpException.serverMessage(): String? =
    runCatching {
        response()?.errorBody()?.string()
            ?.let { errorJson.decodeFromString<ApiErrorBody>(it).message }
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

fun Throwable.toNetworkError(): NetworkError {
    return when (this) {
        is HttpException -> {
            when (code()) {
                400 -> NetworkError.BadRequest(serverMessage())
                401 -> NetworkError.Unauthorized
                403 -> NetworkError.Forbidden
                404 -> NetworkError.NotFound
                409 -> NetworkError.Conflict(serverMessage())
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
        is NetworkError.BadRequest -> serverMessage ?: "Los datos introducidos no son válidos."
        is NetworkError.Unauthorized -> "Correo o contraseña incorrectos."
        is NetworkError.Forbidden -> "No tienes permisos para realizar esta acción."
        is NetworkError.NotFound -> "No se ha encontrado el recurso solicitado."
        is NetworkError.Conflict -> serverMessage ?: "La operación entra en conflicto con el estado actual de los datos."
        is NetworkError.ServerError -> "No se ha podido completar la operación. Inténtalo de nuevo más tarde."
        is NetworkError.ConnectionError -> "No se ha podido conectar con el servidor. Comprueba tu conexión a la red."
        is NetworkError.Unknown -> message ?: "Ha ocurrido un error inesperado."
    }
}
