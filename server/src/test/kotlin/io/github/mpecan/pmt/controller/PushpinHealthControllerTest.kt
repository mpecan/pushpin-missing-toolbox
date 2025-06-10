package io.github.mpecan.pmt.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PushpinHealthControllerTest {
    private val controller = PushpinHealthController()

    @Test
    fun `should return OK for health check`() {
        val result = controller.healthCheck()

        assertEquals("OK", result)
    }

    @Test
    fun `should always return same health check response`() {
        // Call multiple times to ensure consistency
        repeat(5) {
            val result = controller.healthCheck()
            assertEquals("OK", result)
        }
    }

    @Test
    fun `should return non-null response`() {
        val result = controller.healthCheck()

        assertEquals("OK", result)
    }

    @Test
    fun `should return string response`() {
        val result = controller.healthCheck()

        assert(result is String)
        assertEquals("OK", result)
    }
}
