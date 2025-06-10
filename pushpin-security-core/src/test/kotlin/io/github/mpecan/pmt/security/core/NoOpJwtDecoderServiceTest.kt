package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NoOpJwtDecoderServiceTest {
    private val service = NoOpJwtDecoderService()

    @Test
    fun `should return a valid JWT decoder`() {
        val decoder = service.getDecoder()

        assertNotNull(decoder)
    }

    @Test
    fun `should return false for JWT enabled check`() {
        val result = service.isJwtEnabled()

        assertFalse(result)
    }

    @Test
    fun `should return none as provider`() {
        val result = service.getProvider()

        assertEquals("none", result)
    }

    @Test
    fun `should return empty string as issuer`() {
        val result = service.getIssuer()

        assertEquals("", result)
    }

    @Test
    fun `should return empty string as audience`() {
        val result = service.getAudience()

        assertEquals("", result)
    }
}
