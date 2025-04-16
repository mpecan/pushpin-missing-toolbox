package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Format for WebSocket messages
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebSocketFormat(
    val content: String? = null,
    @get:JsonProperty("content-bin")
    val contentBin: String? = null,
    val type: String = "text",
    val action: String = "send"
) : PushpinFormat