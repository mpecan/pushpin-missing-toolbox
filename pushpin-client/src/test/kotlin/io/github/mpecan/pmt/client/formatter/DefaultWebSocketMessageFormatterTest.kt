package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.WebSocketFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class DefaultWebSocketMessageFormatterTest {

    @Mock
    private lateinit var serializationService: MessageSerializationService

    private lateinit var formatter: DefaultWebSocketMessageFormatter

    @BeforeEach
    fun setUp() {
        formatter = DefaultWebSocketMessageFormatter(serializationService)
    }

    @Test
    fun `format should serialize message and set action to send`() {
        // Given
        val message = Message.simple("test-channel", "Hello, World!")
        val serializedMessage = """{"channel":"test-channel","data":"Hello, World!"}"""
        `when`(serializationService.serialize(message)).thenReturn(serializedMessage)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(WebSocketFormat(content = serializedMessage, action = "send", type = "text"), result)
    }

    @Test
    fun `format should handle complex data`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.simple("test-channel", complexData)
        val serializedMessage = """{"channel":"test-channel","data":{"key1":"value1","key2":42,"key3":[1,2,3]}}"""
        `when`(serializationService.serialize(message)).thenReturn(serializedMessage)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(WebSocketFormat(content = serializedMessage, action = "send", type = "text"), result)
    }

    @Test
    fun `format should handle message with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        val serializedMessage = """{"channel":"test-channel","event":"test-event","data":"Hello, World!"}"""
        `when`(serializationService.serialize(message)).thenReturn(serializedMessage)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(WebSocketFormat(content = serializedMessage, action = "send", type = "text"), result)
    }

    @Test
    fun `format should handle message with metadata`() {
        // Given
        val metadata = mapOf("userId" to "123", "timestamp" to 1625097600000)
        val message = Message.withMeta("test-channel", "Hello, World!", metadata)
        val serializedMessage = """{"channel":"test-channel","data":"Hello, World!","meta":{"userId":"123","timestamp":1625097600000}}"""
        `when`(serializationService.serialize(message)).thenReturn(serializedMessage)

        // When
        val result = formatter.format(message)

        // Then
        assertEquals(WebSocketFormat(content = serializedMessage, action = "send", type = "text"), result)
    }
}