package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Format for HTTP responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HttpResponseFormat(
    val code: Int = 200,
    val reason: String? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    @get:JsonProperty("body-bin")
    val bodyBin: String? = null,
    val action: String = "send",
) : PushpinFormat
