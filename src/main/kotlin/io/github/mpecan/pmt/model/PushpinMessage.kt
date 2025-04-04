package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PushpinFormat(
    val content: String,
    val action: String? = null,
    val code: Int? = null,
    val reason: String? = null
)

/**
 *
 * Represents a message to be sent to a Pushpin server.
 * valid formats are
 */
data class PushpinMessage(
    val channel: String,
    val id: String,
    val formats: Map<String, PushpinFormat>
)
