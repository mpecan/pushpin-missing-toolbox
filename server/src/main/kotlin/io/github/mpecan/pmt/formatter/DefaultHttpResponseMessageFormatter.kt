package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.springframework.stereotype.Component

/**
 * Default implementation of HttpResponseMessageFormatter.
 */
@Component
class DefaultHttpResponseMessageFormatter(
    private val serializationService: MessageSerializationService
) : HttpResponseMessageFormatter {
    override fun format(message: Message): PushpinFormat {
        return PushpinFormat(
            body = serializationService.serialize(message.data)
        )
    }
}
