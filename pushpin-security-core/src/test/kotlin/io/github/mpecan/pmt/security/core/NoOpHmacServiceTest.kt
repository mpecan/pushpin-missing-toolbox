package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoOpHmacServiceTest {
    private val service = NoOpHmacService()

    @Test
    fun `should return empty string for signature generation`() {
        val result = service.generateSignature("test-data")

        assertEquals("", result)
    }

    @Test
    fun `should always return true for signature verification`() {
        val result = service.verifySignature("test-data", "any-signature")

        assertTrue(result)
    }

    @Test
    fun `should return empty string for request signature generation`() {
        val result = service.generateRequestSignature("body", 1234567890L, "/api/test")

        assertEquals("", result)
    }

    @Test
    fun `should always return true for request signature verification`() {
        val result =
            service.verifyRequestSignature(
                "body",
                1234567890L,
                "/api/test",
                "any-signature",
                300000L,
            )

        assertTrue(result)
    }

    @Test
    fun `should return false for HMAC enabled check`() {
        val result = service.isHmacEnabled()

        assertFalse(result)
    }

    @Test
    fun `should return default algorithm`() {
        val result = service.getAlgorithm()

        assertEquals("HmacSHA256", result)
    }

    @Test
    fun `should return default header name`() {
        val result = service.getHeaderName()

        assertEquals("X-Pushpin-Signature", result)
    }
}
