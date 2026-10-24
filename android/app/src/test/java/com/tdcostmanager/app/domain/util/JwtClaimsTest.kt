package com.tdcostmanager.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {

    private fun token(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.firma"
    }

    @Test
    fun readsTheSubject() {
        assertEquals("demo@taller3d.es", JwtClaims.subject(token("""{"sub":"demo@taller3d.es","exp":1}""")))
    }

    @Test
    fun returnsNullForMalformedTokens() {
        assertNull(JwtClaims.subject("no-es-un-jwt"))
        assertNull(JwtClaims.subject(token("""{"exp":1}""")))
        assertNull(JwtClaims.subject("a.%%%.c"))
    }
}
