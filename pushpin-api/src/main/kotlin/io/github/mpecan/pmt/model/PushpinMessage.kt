package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a message to be sent to a Pushpin server.
 * * @property channel The channel to publish the message to
 * @property id Optional message identifier for tracking
 * @property prevId Optional identifier of the previous message in sequence
 * @property formats Map of format names to format specifications
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PushpinMessage(
    val channel: String,
    val id: String? = null,
    @get:JsonProperty("prev-id")
    val prevId: String? = null,
    val formats: Map<String, PushpinFormat>,
)
