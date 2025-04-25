package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.WebSocketFormat

/**
 * Default implementation of WebSocketMessageFormatter.
 */
class DefaultWebSocketMessageFormatter(
    private val serializationService: MessageSerializationService
) : WebSocketMessageFormatter {
    override fun format(message: Message): WebSocketFormat {
        return WebSocketFormat(
            content = serializationService.serialize(message),
            type = "text",
            action = "send"
        )
    }
}