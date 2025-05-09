package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpStreamFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class SimpleHttpStreamMessageFormatterTest {

    @Mock
    private lateinit var serializationService: MessageSerializationService

    private lateinit var formatter: SimpleHttpStreamMessageFormatter

    @BeforeEach
    fun setUp() {
        formatter = SimpleHttpStreamMessageFormatter(serializationService)
    }

    @Test
    fun `format should use string data directly`() {
        // Given
        val message = Message.simple("test-channel", "Hello, World!")

        // When
        val result = formatter.format(message)

        // Then
        // String data should be used directly without serialization
        assertEquals(HttpStreamFormat(content = "Hello, World!\n", action = "send"), result)
    }

    @Test
    fun `format should serialize complex data`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.simple("test-channel", complexData)
        val serializedData = """{"key1":"value1","key2":42,"key3":[1,2,3]}"""
        `when`(serializationService.serialize(complexData)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpStreamFormat(content = serializedData + "\n", action = "send"), result)
    }

    @Test
    fun `format should serialize numeric data`() {
        // Given
        val message = Message.simple("test-channel", 42)
        val serializedData = "42"
        `when`(serializationService.serialize(42)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpStreamFormat(content = serializedData + "\n", action = "send"), result)
    }

    @Test
    fun `format should serialize boolean data`() {
        // Given
        val message = Message.simple("test-channel", true)
        val serializedData = "true"
        `when`(serializationService.serialize(true)).thenReturn(serializedData)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(HttpStreamFormat(content = serializedData + "\n", action = "send"), result)
    }

    @Test
    fun `format should handle message with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")

        // When
        val result = formatter.format(message)

        // Then
        // Event type should be ignored for HTTP stream format
        assertEquals(HttpStreamFormat(content = "Hello, World!\n", action = "send"), result)
    }
}
