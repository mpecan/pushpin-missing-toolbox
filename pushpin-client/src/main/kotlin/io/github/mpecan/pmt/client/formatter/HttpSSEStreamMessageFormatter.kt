package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpStreamFormat

/**
 * Implementation of SSEStreamMessageFormatter for Server-Sent Events (SSE).
 */
class HttpSSEStreamMessageFormatter(
    private val serializationService: MessageSerializationService,
) : SSEStreamMessageFormatter {
    override fun format(message: Message): HttpStreamFormat {
        // Handle string data differently to avoid extra quotes
        val data =
            when (message.data) {
                is String -> message.data
                else -> serializationService.serialize(message.data)
            }

        val content =
            listOf(
                message.eventType?.let {
                    "event: ${message.eventType}\n"
                } ?: "",
                "data: $data\n\n",
            ).joinToString("")

        return HttpStreamFormat(
            content = content,
            action = "send",
        )
    }
}
