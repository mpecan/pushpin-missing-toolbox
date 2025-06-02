package io.github.mpecan.pmt.security.jwt

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultJwtDecoderServiceTest {
    @Test
    fun `should return correct configuration values`() {
        val properties =
            JwtProperties(
                enabled = true,
                provider = "symmetric",
                secret = "test-secret-key-32-characters-long",
                issuer = "test-issuer",
                audience = "test-audience",
            )

        val service = DefaultJwtDecoderService(properties)

        assertTrue(service.isJwtEnabled())
        assertEquals("symmetric", service.getProvider())
        assertEquals("test-issuer", service.getIssuer())
        assertEquals("test-audience", service.getAudience())
    }

    @Test
    fun `should create symmetric decoder with valid secret`() {
        val properties =
            JwtProperties(
                enabled = true,
                provider = "symmetric",
                secret = "test-secret-key-32-characters-long",
            )

        val service = DefaultJwtDecoderService(properties)
        val decoder = service.getDecoder()

        assertNotNull(decoder)
    }

    @Test
    fun `should throw exception for symmetric decoder with short secret`() {
        val properties =
            JwtProperties(
                enabled = true,
                provider = "symmetric",
                secret = "short",
            )

        val service = DefaultJwtDecoderService(properties)

        assertThrows<IllegalStateException> {
            service.getDecoder()
        }
    }

    @Test
    fun `should throw exception for oauth2 provider without jwks uri`() {
        val properties =
            JwtProperties(
                enabled = true,
                provider = "oauth2",
                jwksUri = "",
            )

        val service = DefaultJwtDecoderService(properties)

        assertThrows<IllegalStateException> {
            service.getDecoder()
        }
    }

    @Test
    fun `should throw exception for unknown provider`() {
        val properties =
            JwtProperties(
                enabled = true,
                provider = "unknown",
            )

        val service = DefaultJwtDecoderService(properties)

        assertThrows<IllegalStateException> {
            service.getDecoder()
        }
    }
}
