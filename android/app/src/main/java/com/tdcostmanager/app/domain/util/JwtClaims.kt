package com.tdcostmanager.app.domain.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Lectura de las claims públicas de un JWT, solo para mostrarlas en la interfaz.
 * No valida la firma: eso lo hace el backend en cada petición.
 */
object JwtClaims {

    /** El `sub` del token, que el backend rellena con el correo del usuario. */
    fun subject(token: String): String? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return null
        val json = String(Base64.getUrlDecoder().decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')))
        (Json.parseToJsonElement(json) as JsonObject)["sub"]?.jsonPrimitive?.content
    }.getOrNull()
}
