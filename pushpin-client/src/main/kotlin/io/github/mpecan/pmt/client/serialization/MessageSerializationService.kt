package io.github.mpecan.pmt.client.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

/**
 * Interface for serializing message data to string.
 */
interface MessageSerializationService {
    /**
     * Serializes data to string.
     *
     * @param data The data to serialize
     * @return The serialized data as string
     */
    fun serialize(data: Any): String
}

/**
 * Implementation of MessageSerializationService using Jackson ObjectMapper.
 */
class JacksonMessageSerializationService(
    private val objectMapper: ObjectMapper,
) : MessageSerializationService {
    // Create a dedicated ObjectMapper configured for compact JSON output
    private val compactMapper = objectMapper.copy().apply {
        // Configure Jackson to minimize the output JSON structure (while preserving content)
        factory.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        factory.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)

        // Disable pretty printing to avoid extra whitespace in structure
        configure(SerializationFeature.INDENT_OUTPUT, false)
    }
    override fun serialize(data: Any): String {
        return compactMapper.writeValueAsString(data)
    }
}
