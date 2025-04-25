package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Represents a message to be sent to a Pushpin server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PushpinMessage(
    val channel: String,
    val id: String? = null,
    val formats: Map<String, PushpinFormat>
)