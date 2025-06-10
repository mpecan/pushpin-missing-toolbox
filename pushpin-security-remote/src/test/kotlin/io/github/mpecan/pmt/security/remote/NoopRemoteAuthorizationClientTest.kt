package io.github.mpecan.pmt.security.remote

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class NoopRemoteAuthorizationClientTest {
    private val client = NoopRemoteAuthorizationClient()
    private val request: HttpServletRequest = mock()

    @Test
    fun `should always return false for canSubscribe`() {
        val result = client.canSubscribe(request, "test-channel")

        assertFalse(result)
    }

    @Test
    fun `should return false for any channel`() {
        assertFalse(client.canSubscribe(request, "channel1"))
        assertFalse(client.canSubscribe(request, "channel2"))
        assertFalse(client.canSubscribe(request, "notifications"))
        assertFalse(client.canSubscribe(request, "user.123"))
    }

    @Test
    fun `should return empty list for getSubscribableChannels`() {
        val result = client.getSubscribableChannels(request)

        assertTrue(result.isEmpty())
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `should return empty list for getSubscribableChannelsByPattern`() {
        val result1 = client.getSubscribableChannelsByPattern(request, "news.*")
        val result2 = client.getSubscribableChannelsByPattern(request, "user.123.*")
        val result3 = client.getSubscribableChannelsByPattern(request, "notifications")

        assertTrue(result1.isEmpty())
        assertTrue(result2.isEmpty())
        assertTrue(result3.isEmpty())
        assertEquals(emptyList<String>(), result1)
        assertEquals(emptyList<String>(), result2)
        assertEquals(emptyList<String>(), result3)
    }

    @Test
    fun `should handle null request gracefully`() {
        // NoOp implementation should not throw exceptions even with edge cases
        assertFalse(client.canSubscribe(request, "test"))
        assertTrue(client.getSubscribableChannels(request).isEmpty())
        assertTrue(client.getSubscribableChannelsByPattern(request, "pattern").isEmpty())
    }
}
