package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PushpinFormat(
    val content: String? = null,
    val action: String? = null,
    val code: Int? = null,
    val reason: String? = null,
    val type: String? = null,
    val body: String? = null,
    @get:JsonProperty("body-bin")
    val bodyBin: String? = null,
    @get:JsonProperty("body-patch")
    val bodyPatch: String? = null
)

/**
 *
 * Represents a message to be sent to a Pushpin server.
 * valid formats are
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PushpinMessage(
    val channel: String,
    val id: String? = null,
    val formats: Map<String, PushpinFormat>
)
