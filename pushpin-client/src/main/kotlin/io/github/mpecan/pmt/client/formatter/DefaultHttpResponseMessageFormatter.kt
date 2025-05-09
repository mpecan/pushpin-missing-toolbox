package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.HttpResponseFormat

/**
 * Default implementation of HttpResponseMessageFormatter.
 */
class DefaultHttpResponseMessageFormatter(
    private val serializationService: MessageSerializationService,
) : HttpResponseMessageFormatter {
    override fun format(message: Message): HttpResponseFormat {
        return HttpResponseFormat(
            body = serializationService.serialize(message.data),
        )
    }
}
