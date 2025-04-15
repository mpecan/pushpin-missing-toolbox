package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base interface for all Pushpin format types
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "formatType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = HttpResponseFormat::class, name = "http-response"),
    JsonSubTypes.Type(value = HttpStreamFormat::class, name = "http-stream"),
    JsonSubTypes.Type(value = WebSocketFormat::class, name = "ws-message")
)
@JsonInclude(JsonInclude.Include.NON_NULL)
interface PushpinFormat {
    // Common properties if needed
}