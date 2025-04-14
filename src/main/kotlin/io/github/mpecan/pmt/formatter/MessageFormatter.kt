package io.github.mpecan.pmt.formatter

import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat

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
interface WebSocketMessageFormatter : MessageFormatter

/**
 * Interface for formatting messages for HTTP stream protocol (SSE).
 */
interface SSEStreamMessageFormatter : MessageFormatter

/**
 * Interface for formatting messages for HTTP stream protocol (SSE) with a specific implementation.
 */
interface HttpStreamMessageFormatter : MessageFormatter

/**
 * Interface for formatting messages for HTTP response protocol.
 */
interface HttpResponseMessageFormatter : MessageFormatter

/**
 * Interface for formatting messages for HTTP long-polling protocol.
 */
interface LongPollingMessageFormatter : MessageFormatter
