package com.tdcostmanager.app.domain.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class NetworkErrorTest {

    private fun httpError(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun conflictKeepsTheServerMessage() {
        val error = httpError(409, """{"status":409,"error":"Conflict","message":"No se puede pasar un presupuesto de SENT a DRAFT","path":"/x"}""")
            .toNetworkError()

        assertEquals(NetworkError.Conflict("No se puede pasar un presupuesto de SENT a DRAFT"), error)
        assertEquals("No se puede pasar un presupuesto de SENT a DRAFT", error.toFriendlyMessage())
    }

    @Test
    fun badRequestWithoutReadableBodyFallsBackToGenericMessage() {
        val error = httpError(400, "<html>Bad Request</html>").toNetworkError()

        assertEquals(NetworkError.BadRequest(null), error)
        assertEquals("Los datos introducidos no son válidos.", error.toFriendlyMessage())
    }

    @Test
    fun otherStatusCodesAndIoErrorsAreClassified() {
        assertEquals(NetworkError.NotFound, httpError(404, "{}").toNetworkError())
        assertEquals(NetworkError.ServerError, httpError(503, "{}").toNetworkError())
        assertEquals(NetworkError.ConnectionError, IOException("timeout").toNetworkError())
    }
}
