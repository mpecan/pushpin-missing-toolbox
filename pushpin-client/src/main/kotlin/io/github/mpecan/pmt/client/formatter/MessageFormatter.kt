package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.exception.MessageFormattingException
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.model.HttpResponseFormat
import io.github.mpecan.pmt.model.HttpStreamFormat
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.model.WebSocketFormat

/**
 * Interface for formatting messages to Pushpin format.
 */
interface MessageFormatter {
    /**
     * Formats a message to Pushpin format.
     *
     * @param message The message to format
     * @return The formatted message in Pushpin format
     * @throws MessageFormattingException If there is an error during formatting
     */
    @Throws(MessageFormattingException::class)
    fun format(message: Message): PushpinFormat
}

/**
 * Interface for formatting messages for WebSocket protocol.
 */
interface WebSocketMessageFormatter : MessageFormatter {
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): WebSocketFormat
}

/**
 * Interface for formatting messages for HTTP stream protocol (SSE).
 */
interface SSEStreamMessageFormatter : MessageFormatter {
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): HttpStreamFormat
}

/**
 * Interface for formatting messages for HTTP stream protocol (SSE) with a specific implementation.
 */
interface HttpStreamMessageFormatter : MessageFormatter {
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): HttpStreamFormat
}

/**
 * Interface for formatting messages for HTTP response protocol.
 */
interface HttpResponseMessageFormatter : MessageFormatter {
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): HttpResponseFormat
}

/**
 * Interface for formatting messages for HTTP long-polling protocol.
 */
interface LongPollingMessageFormatter : MessageFormatter {
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): HttpResponseFormat
}
