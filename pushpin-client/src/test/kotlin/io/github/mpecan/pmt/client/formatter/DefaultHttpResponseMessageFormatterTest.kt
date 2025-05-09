package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpResponseFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class DefaultHttpResponseMessageFormatterTest {

    private val serializationService: MessageSerializationService = mock()

    private lateinit var formatter: DefaultHttpResponseMessageFormatter

    @BeforeEach
    fun setUp() {
        formatter = DefaultHttpResponseMessageFormatter(serializationService)
    }

    @Test
    fun `format should serialize data as body`() {
        // Given
        val message = Message.simple("test-channel", "Hello, World!")
        val serializedData = "\"Hello, World!\""
        whenever(serializationService.serialize(message.data)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle complex data`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.simple("test-channel", complexData)
        val serializedData = """{"key1":"value1","key2":42,"key3":[1,2,3]}"""
        whenever(serializationService.serialize(complexData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle numeric data`() {
        // Given
        val message = Message.simple("test-channel", 42)
        val serializedData = "42"
        whenever(serializationService.serialize(42)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle boolean data`() {
        // Given
        val message = Message.simple("test-channel", true)
        val serializedData = "true"
        whenever(serializationService.serialize(true)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpResponseFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle message with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        val serializedData = "\"Hello, World!\""
        whenever(serializationService.serialize(message.data)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        // Event type should be ignored for HTTP response format
        assertEquals(HttpResponseFormat(body = serializedData), result)
    }
}
