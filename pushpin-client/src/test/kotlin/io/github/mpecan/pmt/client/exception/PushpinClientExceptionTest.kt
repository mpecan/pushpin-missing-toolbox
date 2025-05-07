package io.github.mpecan.pmt.client.exception

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushpinClientExceptionTest {

    @Test
    fun `PushpinClientException accepts message and cause`() {
        val cause = RuntimeException("Original error")
        val exception = PushpinClientException("Test exception", cause)
        
        assertEquals("Test exception", exception.message)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `MessageSerializationException extends PushpinClientException`() {
        val exception = MessageSerializationException("Serialization failed")
        
        assertTrue(exception is PushpinClientException)
        assertEquals("Serialization failed", exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `MessageFormattingException extends PushpinClientException`() {
        val exception = MessageFormattingException("Formatting failed")
        
        assertTrue(exception is PushpinClientException)
        assertEquals("Formatting failed", exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `PublishingException formats message with server info and status code`() {
        val exception = PublishingException(
            message = "Failed to publish",
            serverInfo = "pushpin-1:7999",
            statusCode = 500
        )
        
        assertTrue(exception is PushpinClientException)
        assertEquals("Failed to publish (Server: pushpin-1:7999, Status: 500)", exception.message)
    }
    
    @Test
    fun `PublishingException formats message with server info only`() {
        val exception = PublishingException(
            message = "Failed to publish",
            serverInfo = "pushpin-1:7999"
        )
        
        assertEquals("Failed to publish (Server: pushpin-1:7999)", exception.message)
    }
    
    @Test
    fun `PublishingException formats message with status code only`() {
        val exception = PublishingException(
            message = "Failed to publish",
            statusCode = 500
        )
        
        assertEquals("Failed to publish (Status: 500)", exception.message)
    }
    
    @Test
    fun `NoServerAvailableException uses default message`() {
        val exception = NoServerAvailableException()
        
        assertEquals("No Pushpin servers available", exception.message)
    }
    
    @Test
    fun `PushpinTimeoutException formats message with server info and timeout`() {
        val exception = PushpinTimeoutException(
            serverInfo = "pushpin-1:7999",
            timeoutMs = 5000
        )
        
        assertEquals("Connection to Pushpin server timed out (Server: pushpin-1:7999, Timeout: 5000ms)", exception.message)
    }
    
    @Test
    fun `PushpinAuthenticationException formats message with server info`() {
        val exception = PushpinAuthenticationException(
            serverInfo = "pushpin-1:7999"
        )
        
        assertEquals("Authentication with Pushpin server failed (Server: pushpin-1:7999)", exception.message)
    }
}