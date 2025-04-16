package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.HttpStreamFormat
import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.serialization.MessageSerializationService

class SimpleHttpStreamMessageFormatter(
    private val serializationService: MessageSerializationService
) : HttpStreamMessageFormatter {
    override fun format(message: Message): HttpStreamFormat {
        // Handle string data differently to avoid extra quotes
        val data = when (message.data) {
            is String -> message.data
            else -> serializationService.serialize(message.data)
        }

        return HttpStreamFormat(
            content = data.toString() + "\n",
            action = "send"
        )
    }
}
