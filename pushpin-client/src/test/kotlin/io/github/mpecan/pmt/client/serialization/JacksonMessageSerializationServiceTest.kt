package io.github.mpecan.pmt.client.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JacksonMessageSerializationServiceTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var serializationService: JacksonMessageSerializationService

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        serializationService = JacksonMessageSerializationService(objectMapper)
    }

    @Test
    fun `serialize should convert string to JSON string`() {
        // Given
        val data = "Hello, World!"

        // When
        val result = serializationService.serialize(data)

        // Then
        assertEquals("\"Hello, World!\"", result)
    }

    @Test
    fun `serialize should convert number to JSON string`() {
        // Given
        val data = 42

        // When
        val result = serializationService.serialize(data)

        // Then
        assertEquals("42", result)
    }

    @Test
    fun `serialize should convert boolean to JSON string`() {
        // Given
        val data = true

        // When
        val result = serializationService.serialize(data)

        // Then
        assertEquals("true", result)
    }

    @Test
    fun `serialize should convert complex object to JSON string`() {
        // Given
        val data = mapOf(
            "name" to "John Doe",
            "age" to 30,
            "isActive" to true,
            "hobbies" to listOf("reading", "coding", "hiking")
        )

        // When
        val result = serializationService.serialize(data)

        // Then
        val expected = """{"name":"John Doe","age":30,"isActive":true,"hobbies":["reading","coding","hiking"]}"""
        assertEquals(expected, result)
    }
}