package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpResponseFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class DefaultLongPollingMessageFormatterTest {

    private val serializationService: MessageSerializationService = mock()

    private lateinit var formatter: DefaultLongPollingMessageFormatter

    @BeforeEach
    fun setUp() {
        formatter = DefaultLongPollingMessageFormatter(serializationService)
    }

    @Test
    fun `format should include channel and message in response`() {
        // Given
        val message = Message.simple("test-channel", "Hello, World!")
        val responseData = mapOf(
            "channel" to "test-channel",
            "message" to "Hello, World!",
        )
        val serializedData = """{"channel":"test-channel","message":"Hello, World!"}"""
        whenever(serializationService.serialize(responseData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData + "\n"), result)
    }

    @Test
    fun `format should handle complex data`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42)
        val message = Message.simple("test-channel", complexData)
        val responseData = mapOf(
            "channel" to "test-channel",
            "message" to "{key1=value1, key2=42}",
        )
        val serializedData = """{"channel":"test-channel","message":"{key1=value1, key2=42}"}"""
        whenever(serializationService.serialize(responseData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData + "\n"), result)
    }

    @Test
    fun `format should handle numeric data`() {
        // Given
        val message = Message.simple("test-channel", 42)
        val responseData = mapOf(
            "channel" to "test-channel",
            "message" to "42",
        )
        val serializedData = """{"channel":"test-channel","message":"42"}"""
        whenever(serializationService.serialize(responseData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData + "\n"), result)
    }

    @Test
    fun `format should handle message with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        val responseData = mapOf(
            "channel" to "test-channel",
            "message" to "Hello, World!",
        )
        val serializedData = """{"channel":"test-channel","message":"Hello, World!"}"""
        whenever(serializationService.serialize(responseData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        // Event type should not be included in the response for long polling
        assertEquals(HttpResponseFormat(body = serializedData + "\n"), result)
    }
}
