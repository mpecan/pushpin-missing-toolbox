package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
/**
 * Represents a message to be published to Pushpin.
 *
 * @property channel The channel to publish the message to
 * @property eventType The type of event (optional)
 * @property data The message data
 * @property meta Additional metadata (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Message(
    @JsonProperty("channel")
    val channel: String,

    @JsonProperty("event")
    val eventType: String? = null,

    @JsonProperty("data")
    val data: Any,

    @JsonProperty("meta")
    val meta: Map<String, Any>? = null,

    @JsonProperty("transports")
    val transports: List<Transport> = listOf(
        Transport.WebSocket,
        Transport.HttpStreamSSE,
        Transport.HttpResponseSSE,
        Transport.LongPolling
    ),
) {
    companion object {
        /**
         * Creates a simple message with just channel and data.
         */
        fun simple(channel: String, data: Any): Message {
            return Message(channel = channel, data = data)
        }

        /**
         * Creates an event message with channel, event type, and data.
         */
        fun event(channel: String, eventType: String, data: Any): Message {
            return Message(channel = channel, eventType = eventType, data = data)
        }

        /**
         * Creates a message with channel, data, and metadata.
         */
        fun withMeta(channel: String, data: Any, meta: Map<String, Any>): Message {
            return Message(channel = channel, data = data, meta = meta)
        }

        /**
         * Creates a complete message with all properties.
         */
        fun complete(channel: String, eventType: String, data: Any, meta: Map<String, Any>): Message {
            return Message(channel = channel, eventType = eventType, data = data, meta = meta)
        }
    }
}
