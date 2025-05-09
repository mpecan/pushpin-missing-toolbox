package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpStreamFormat

class SimpleHttpStreamMessageFormatter(
    private val serializationService: MessageSerializationService,
) : HttpStreamMessageFormatter {
    override fun format(message: Message): HttpStreamFormat {
        // Handle string data differently to avoid extra quotes
        val data = when (message.data) {
            is String -> message.data
            else -> serializationService.serialize(message.data)
        }

        return HttpStreamFormat(
            content = data + "\n",
        )
    }
}
