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
    private val longPollingFormatter: LongPollingMessageFormatter,
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
            // Create a map of format names to format instances for enabled transports
            val formatMap = mutableMapOf<String, io.github.mpecan.pmt.model.PushpinFormat>()

            // Always include WebSocket format as per GRIP protocol recommendations
            formatMap["ws-message"] = webSocketFormatter.format(message)

            // Add HTTP Stream format if one of the HTTP stream transports is enabled
            if (message.transports.contains(Transport.HttpStream) || message.transports.contains(
                    Transport.HttpStreamSSE,
                )
            ) {
                formatMap["http-stream"] = if (message.transports.contains(Transport.HttpStream)) {
                    httpStreamMessageFormatter.format(message)
                } else {
                    httpSseStreamFormatter.format(message)
                }
            }

            // Add HTTP Response format if one of the HTTP response transports is enabled
            if (message.transports.contains(Transport.HttpResponse) || message.transports.contains(
                    Transport.HttpResponseSSE,
                ) ||
                message.transports.contains(Transport.LongPolling)
            ) {
                formatMap["http-response"] = if (message.transports.contains(Transport.LongPolling)) {
                    longPollingFormatter.format(message)
                } else {
                    httpResponseFormatter.format(message)
                }
            }

            return PushpinMessage(
                channel = message.channel,
                id = message.id,
                prevId = message.prevId,
                formats = formatMap,
            )
        } catch (e: MessageFormattingException) {
            throw MessageSerializationException("Failed to serialize message due to formatting error", e)
        } catch (e: Exception) {
            throw MessageSerializationException("Failed to serialize message", e)
        }
    }
}
