package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Format for HTTP stream
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HttpStreamFormat(
    val content: String? = null,
    @get:JsonProperty("content-bin")
    val contentBin: String? = null,
    val action: String = "send"
) : PushpinFormat