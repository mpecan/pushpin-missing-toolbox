package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.springframework.stereotype.Component

/**
 * Implementation of HttpStreamMessageFormatter that formats messages as Server-Sent Events (SSE).
 */
@Component
class HttpSSEStreamMessageFormatter(
    private val serializationService: MessageSerializationService
) : SSEStreamMessageFormatter {
    override fun format(message: Message): PushpinFormat {
        // Handle string data differently to avoid extra quotes
        val data = when (message.data) {
            is String -> message.data as String
            else -> serializationService.serialize(message.data)
        }

        val content = if (message.eventType != null) {
            "event: ${message.eventType}\ndata: $data\n\n"
        } else {
            "data: $data\n\n"
        }
        return PushpinFormat(
            content = content,
            action = "send"
        )
    }
}