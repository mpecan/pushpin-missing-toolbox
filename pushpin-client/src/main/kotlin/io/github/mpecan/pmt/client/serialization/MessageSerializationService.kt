package io.github.mpecan.pmt.client.serialization

import com.fasterxml.jackson.databind.ObjectMapper

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
    private val objectMapper: ObjectMapper
) : MessageSerializationService {
    override fun serialize(data: Any): String {
        return objectMapper.writeValueAsString(data)
    }
}
