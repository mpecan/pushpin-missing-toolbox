package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpResponseFormat

/**
 * Default implementation of LongPollingMessageFormatter.
 *
 * This formatter creates a JSON response for long-polling requests.
 */
class DefaultLongPollingMessageFormatter(
    private val serializationService: MessageSerializationService,
) : LongPollingMessageFormatter {
    override fun format(message: Message): HttpResponseFormat {
        // For long-polling, we need to include the channel in the response
        val responseData = mapOf(
            "channel" to message.channel,
            "message" to "${message.data}",
        )

        return HttpResponseFormat(
            body = serializationService.serialize(responseData) + "\n",
        )
    }
}
