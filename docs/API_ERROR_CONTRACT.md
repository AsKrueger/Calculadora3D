# Contrato de Respuestas de Error de la API REST — 3D Cost Manager

Este documento define la estructura estandarizada de respuestas de error que devuelve el backend de **3D Cost Manager** ante cualquier condición anómala o excepción durante las peticiones HTTP.

---

## 1. Estructura Unificada de Error (`ApiError`)

Todas las respuestas de error utilizan una estructura JSON JSON-schema predecible basada en la clase `ApiError`:

```json
{
  "timestamp": "2026-10-10T14:35:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: email: no puede estar vacío",
  "path": "/api/v1/auth/register",
  "validationErrors": {
    "email": "no puede estar vacío"
  }
}
```

### Campos

| Campo | Tipo | Descripción | Opcional |
|---|---|---|---|
| `timestamp` | String (ISO-8601) | Marca de tiempo exacta en que se generó la respuesta de error | No |
| `status` | Integer | Código numérico de estado HTTP (ej. 400, 401, 404, 409, 500) | No |
| `error` | String | Frase corta estandarizada asociada al código HTTP | No |
| `message` | String | Mensaje seguro en lenguaje natural accesible para el cliente | No |
| `path` | String | URI relativa del recurso consultado en la petición | No |
| `validationErrors` | Map<String, String> | Diccionario con errores específicos por campo (solo en validaciones 400) | Sí (se omite si es `null`) |

---

## 2. Correspondencia de Códigos de Estado HTTP

| Excepción / Condición | Código HTTP | `error` | Ejemplo de Mensaje |
|---|---|---|---|
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | "Bad Request" | `"Validation failed: name: es obligatorio"` |
| `IllegalArgumentException` | `400 BAD_REQUEST` | "Bad Request" | `"Parámetro o argumento no válido"` |
| `HttpMessageNotReadableException` | `400 BAD_REQUEST` | "Bad Request" | `"Cuerpo de la petición JSON malformado o no válido."` |
| `BadCredentialsException` | `401 UNAUTHORIZED` | "Unauthorized" | `"Credenciales inválidas"` |
| `AccessDeniedException` | `403 FORBIDDEN` | "Forbidden" | `"No tiene permisos para acceder a este recurso"` |
| `EntityNotFoundException` | `404 NOT_FOUND` | "Not Found" | `"Proyecto no encontrado con ID: 12"` |
| `HttpRequestMethodNotSupportedException` | `405 METHOD_NOT_ALLOWED` | "Method Not Allowed" | `"El método HTTP POST no está soportado para esta ruta."` |
| `IllegalStateException` | `409 CONFLICT` | "Conflict" | `"Conflicto de estado en la solicitud"` |
| `DataIntegrityViolationException` | `409 CONFLICT` | "Conflict" | `"El recurso solicitado genera un conflicto de datos o duplicidad."` |
| `Exception` / `RuntimeException` general | `500 INTERNAL_SERVER_ERROR` | "Internal Server Error" | `"Ha ocurrido un error interno e inesperado en el servidor."` |
| Fallo de comunicación ESIOS | `503 SERVICE_UNAVAILABLE` | "Service Unavailable" | `"Fallo en la comunicación técnica HTTP con ESIOS"` |

---

## 3. Seguridad y Privacidad en Errores

1. **Sin filtrado de trazas internas:** Las respuestas HTTP 500 nunca exponen trazas de pila (`stackTrace`), nombres de clases internas Java, ni mensajes de error de consultas SQL nativas.
2. **Registro interno seguro (`log.error`):** Los detalles de la excepción completa se registran únicamente en los logs del servidor mediante Slf4j para diagnóstico interno.
3. **Sin secretos:** Nunca se incluyen tokens JWT, secretos ni credenciales en la respuesta de error.

---

## 4. Integración con el Cliente Android

En el cliente Android (`com.tdcostmanager.app`), Retrofit captura los errores HTTP. La función `toNetworkError()` en `NetworkError.kt` mapea los códigos HTTP (`400`, `401`, `403`, `404`, `409`, `500..599`) a mensajes amigables locales para el usuario en la interfaz Compose.
