package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.springframework.stereotype.Component

/**
 * Default implementation of LongPollingMessageFormatter.
 * 
 * This formatter creates a JSON response for long-polling requests.
 */
@Component
class DefaultLongPollingMessageFormatter(
    private val serializationService: MessageSerializationService
) : LongPollingMessageFormatter {
    override fun format(message: Message): PushpinFormat {
        // For long-polling, we need to include the channel in the response
        val responseData = mapOf(
            "channel" to message.channel,
            "message" to  "${message.data}",
        )
        
        return PushpinFormat(
            body = serializationService.serialize(responseData) + "\n",
        )
    }
}