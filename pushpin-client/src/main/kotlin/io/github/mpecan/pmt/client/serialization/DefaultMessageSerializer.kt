package io.github.mpecan.pmt.client.serialization

import io.github.mpecan.pmt.client.exception.MessageFormattingException
import io.github.mpecan.pmt.client.exception.MessageSerializationException
import io.github.mpecan.pmt.client.formatter.*
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.model.Transport
import io.github.mpecan.pmt.model.PushpinMessage

/**
 * Default implementation of MessageSerializer.
 * Converts a Message to a PushpinMessage using the configured formatters.
 */
class DefaultMessageSerializer(
    private val webSocketFormatter: WebSocketMessageFormatter,
    private val httpSseStreamFormatter: SSEStreamMessageFormatter,
    private val httpStreamMessageFormatter: HttpStreamMessageFormatter,
    private val httpResponseFormatter: HttpResponseMessageFormatter,
    private val longPollingFormatter: LongPollingMessageFormatter
) : MessageSerializer {

    /**
     * Converts a Message to a PushpinMessage using the configured formatters.
     *
     * @param message The message to convert
     * @return The converted PushpinMessage
     * @throws MessageSerializationException If there is an error during serialization
     */
    @Throws(MessageSerializationException::class)
    override fun serialize(message: Message): PushpinMessage {
        try {
            return PushpinMessage(
                channel = message.channel,
                formats = mapOf(
                    "ws-message" to webSocketFormatter.format(message),
                    "http-stream" to when {
                        message.transports.contains(Transport.HttpStream) -> httpStreamMessageFormatter.format(
                            message
                        )

                        else -> httpSseStreamFormatter.format(message)
                    },
                    "http-response" to when {
                        message.transports.contains(Transport.LongPolling) -> longPollingFormatter.format(
                            message
                        )

                        else -> httpResponseFormatter.format(message)
                    }
                )
            )
        } catch (e: MessageFormattingException) {
            throw MessageSerializationException("Failed to serialize message due to formatting error", e)
        } catch (e: Exception) {
            throw MessageSerializationException("Failed to serialize message", e)
        }
    }
}