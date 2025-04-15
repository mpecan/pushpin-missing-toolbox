package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.HttpStreamFormat
import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class HttpSSEStreamMessageFormatterTest {

    @Mock
    private lateinit var serializationService: MessageSerializationService

    private lateinit var formatter: HttpSSEStreamMessageFormatter

    @BeforeEach
    fun setUp() {
        formatter = HttpSSEStreamMessageFormatter(serializationService)
    }

    @Test
    fun `format should handle string data without event type`() {
        // Given
        val message = Message.simple("test-channel", "Hello, World!")
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "data: Hello, World!\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }

    @Test
    fun `format should handle string data with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "event: test-event\ndata: Hello, World!\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }

    @Test
    fun `format should handle complex data without event type`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.simple("test-channel", complexData)
        val serializedData = """{"key1":"value1","key2":42,"key3":[1,2,3]}"""
        `when`(serializationService.serialize(complexData)).thenReturn(serializedData)
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "data: ${serializedData}\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }

    @Test
    fun `format should handle complex data with event type`() {
        // Given
        val complexData = mapOf("key1" to "value1", "key2" to 42, "key3" to listOf(1, 2, 3))
        val message = Message.event("test-channel", "test-event", complexData)
        val serializedData = """{"key1":"value1","key2":42,"key3":[1,2,3]}"""
        `when`(serializationService.serialize(complexData)).thenReturn(serializedData)
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "event: test-event\ndata: ${serializedData}\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }

    @Test
    fun `format should handle numeric data without event type`() {
        // Given
        val message = Message.simple("test-channel", 42)
        `when`(serializationService.serialize(42)).thenReturn("42")
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "data: 42\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }

    @Test
    fun `format should handle boolean data with event type`() {
        // Given
        val message = Message.event("test-channel", "test-event", true)
        `when`(serializationService.serialize(true)).thenReturn("true")
        
        // When
        val result = formatter.format(message)

        // Then
        val expectedContent = "event: test-event\ndata: true\n\n"
        assertEquals(HttpStreamFormat(content = expectedContent, action = "send"), result)
    }
}