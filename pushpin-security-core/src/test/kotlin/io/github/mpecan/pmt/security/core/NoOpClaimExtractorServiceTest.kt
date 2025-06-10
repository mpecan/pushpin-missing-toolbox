package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.security.oauth2.jwt.Jwt

class NoOpClaimExtractorServiceTest {
    private val service = NoOpClaimExtractorService()

    @Test
    fun `should return null when extracting string claim`() {
        val jwt: Jwt = mock()

        val result = service.extractStringClaim(jwt, "$.user.name")

        assertNull(result)
    }

    @Test
    fun `should return empty list when extracting list claim`() {
        val jwt: Jwt = mock()

        val result = service.extractListClaim(jwt, "$.user.roles")

        assertTrue(result.isEmpty())
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `should return empty map when extracting map claim`() {
        val jwt: Jwt = mock()

        val result = service.extractMapClaim(jwt, "$.user.metadata")

        assertTrue(result.isEmpty())
        assertEquals(emptyMap<String, Any>(), result)
    }

    @Test
    fun `should return false when checking if claim exists`() {
        val jwt: Jwt = mock()

        val result = service.hasClaim(jwt, "$.user.id")

        assertFalse(result)
    }
}
