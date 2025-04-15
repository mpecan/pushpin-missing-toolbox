package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.WebSocketFormat
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.springframework.stereotype.Component

/**
 * Default implementation of WebSocketMessageFormatter.
 */
@Component
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