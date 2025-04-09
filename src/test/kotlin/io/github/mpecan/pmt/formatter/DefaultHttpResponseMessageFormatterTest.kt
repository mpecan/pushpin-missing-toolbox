package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class DefaultHttpResponseMessageFormatterTest {

    @Mock
    private lateinit var serializationService: MessageSerializationService

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
        `when`(serializationService.serialize(message.data)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(PushpinFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle complex data`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.simple("test-channel", complexData)
        val serializedData = """{"key1":"value1","key2":42,"key3":[1,2,3]}"""
        `when`(serializationService.serialize(complexData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(PushpinFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle numeric data`() {
        // Given
        val message = Message.simple("test-channel", 42)
        val serializedData = "42"
        `when`(serializationService.serialize(42)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(PushpinFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle boolean data`() {
        // Given
        val message = Message.simple("test-channel", true)
        val serializedData = "true"
        `when`(serializationService.serialize(true)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(PushpinFormat(body = serializedData), result)
    }

    @Test
    fun `format should handle message with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        val serializedData = "\"Hello, World!\""
        `when`(serializationService.serialize(message.data)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        // Event type should be ignored for HTTP response format
        assertEquals(PushpinFormat(body = serializedData), result)
    }
}