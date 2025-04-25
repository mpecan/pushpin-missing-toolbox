package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.model.*

/**
 * Interface for formatting messages to Pushpin format.
 */
interface MessageFormatter {
    /**
     * Formats a message to Pushpin format.
     *
     * @param message The message to format
     * @return The formatted message in Pushpin format
     */
    fun format(message: Message): PushpinFormat
}

/**
 * Interface for formatting messages for WebSocket protocol.
 */
interface WebSocketMessageFormatter : MessageFormatter {
    override fun format(message: Message): WebSocketFormat
}

/**
 * Interface for formatting messages for HTTP stream protocol (SSE).
 */
interface SSEStreamMessageFormatter : MessageFormatter {
    override fun format(message: Message): HttpStreamFormat
}

/**
 * Interface for formatting messages for HTTP stream protocol (SSE) with a specific implementation.
 */
interface HttpStreamMessageFormatter : MessageFormatter {
    override fun format(message: Message): HttpStreamFormat
}

/**
 * Interface for formatting messages for HTTP response protocol.
 */
interface HttpResponseMessageFormatter : MessageFormatter {
    override fun format(message: Message): HttpResponseFormat
}

/**
 * Interface for formatting messages for HTTP long-polling protocol.
 */
interface LongPollingMessageFormatter : MessageFormatter {
    override fun format(message: Message): HttpResponseFormat
}