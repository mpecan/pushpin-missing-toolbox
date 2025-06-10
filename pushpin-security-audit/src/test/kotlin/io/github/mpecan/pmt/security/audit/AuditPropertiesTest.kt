package io.github.mpecan.pmt.security.audit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuditPropertiesTest {
    @Test
    fun `should have default values`() {
        val properties = AuditProperties()

        assertFalse(properties.enabled)
        assertEquals("INFO", properties.level)
        assertFalse(properties.includeStackTrace)
        assertTrue(properties.logSuccessfulAuth)
        assertTrue(properties.logFailedAuth)
        assertTrue(properties.logChannelAccess)
        assertTrue(properties.logAdminActions)
        assertEquals(1000, properties.maxMessageLength)
        assertFalse(properties.includeRequestHeaders)
        assertEquals(listOf("Authorization", "Cookie", "X-Auth-Token"), properties.excludeHeaders)
    }

    @Test
    fun `should allow custom values`() {
        val properties =
            AuditProperties(
                enabled = true,
                level = "DEBUG",
                includeStackTrace = true,
                logSuccessfulAuth = false,
                maxMessageLength = 500,
                excludeHeaders = listOf("Custom-Header"),
            )

        assertTrue(properties.enabled)
        assertEquals("DEBUG", properties.level)
        assertTrue(properties.includeStackTrace)
        assertFalse(properties.logSuccessfulAuth)
        assertEquals(500, properties.maxMessageLength)
        assertEquals(listOf("Custom-Header"), properties.excludeHeaders)
    }

    @Test
    fun `should create copy with modified values`() {
        val original = AuditProperties()
        val modified = original.copy(enabled = true, level = "ERROR")

        assertFalse(original.enabled)
        assertEquals("INFO", original.level)
        assertTrue(modified.enabled)
        assertEquals("ERROR", modified.level)
        // Other values should remain the same
        assertEquals(original.includeStackTrace, modified.includeStackTrace)
        assertEquals(original.maxMessageLength, modified.maxMessageLength)
    }
}
