package io.github.mpecan.pmt.client.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
/**
 * Represents a message to be published to Pushpin.
 *
 * @property channel The channel to publish the message to
 * @property eventType The type of event (optional)
 * @property data The message data
 * @property meta Additional metadata (optional)
 * @property id Optional message identifier for tracking
 * @property prevId Optional identifier of the previous message in sequence
 * @property transports List of transports to be used (default: WebSocket, HttpStreamSSE, HttpResponseSSE, LongPolling)
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

    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("prev-id")
    val prevId: String? = null,

    @JsonProperty("transports")
    val transports: List<Transport> = listOf(
        Transport.WebSocket,
        Transport.HttpStreamSSE,
        Transport.HttpResponseSSE,
        Transport.LongPolling,
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
         * Creates a message with specific transports.
         */
        fun withTransports(channel: String, data: Any, transports: List<Transport>): Message {
            return Message(channel = channel, data = data, transports = transports)
        }

        /**
         * Creates a WebSocket-only message.
         */
        fun webSocketOnly(channel: String, data: Any): Message {
            return Message(channel = channel, data = data, transports = listOf(Transport.WebSocket))
        }

        /**
         * Creates an HTTP-stream-only message.
         */
        fun httpStreamOnly(channel: String, data: Any): Message {
            return Message(channel = channel, data = data, transports = listOf(Transport.HttpStream))
        }

        /**
         * Creates an SSE-only message.
         */
        fun sseOnly(channel: String, data: Any): Message {
            return Message(channel = channel, data = data, transports = listOf(Transport.HttpStreamSSE))
        }

        /**
         * Creates a message with tracking IDs.
         */
        fun withIds(channel: String, data: Any, id: String, prevId: String? = null): Message {
            return Message(channel = channel, data = data, id = id, prevId = prevId)
        }

        /**
         * Creates a fully customized message with all properties.
         */
        fun custom(
            channel: String,
            data: Any,
            eventType: String? = null,
            meta: Map<String, Any>? = null,
            id: String? = null,
            prevId: String? = null,
            transports: List<Transport>? = null,
        ): Message {
            return Message(
                channel = channel, data = data, eventType = eventType, meta = meta,
                id = id,
                prevId = prevId,
                transports = transports ?: listOf(
                    Transport.WebSocket,
                    Transport.HttpStreamSSE,
                    Transport.HttpResponseSSE,
                    Transport.LongPolling,
                ),
            )
        }
    }

    /**
     * Creates a copy of this message with additional metadata.
     */
    fun addMeta(additionalMeta: Map<String, Any>): Message {
        val newMeta = when {
            meta != null -> meta + additionalMeta
            else -> additionalMeta
        }
        return copy(meta = newMeta)
    }

    /**
     * Creates a copy of this message with a different event type.
     */
    fun withEventType(newEventType: String): Message {
        return copy(eventType = newEventType)
    }

    /**
     * Creates a copy of this message with different transports.
     */
    fun withTransports(newTransports: List<Transport>): Message {
        return copy(transports = newTransports)
    }

    /**
     * Creates a copy of this message with message tracking IDs.
     */
    fun withIds(id: String, prevId: String? = null): Message {
        return copy(id = id, prevId = prevId)
    }
}
