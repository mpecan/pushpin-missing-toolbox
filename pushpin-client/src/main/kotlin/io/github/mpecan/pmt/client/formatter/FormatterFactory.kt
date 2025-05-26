package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.serialization.MessageSerializationService

/**
 * Factory for creating custom message formatters.
 */
interface FormatterFactory {
    /**
     * Creates a WebSocket message formatter with custom options.
     */
    fun createWebSocketFormatter(options: FormatterOptions = FormatterOptions()): WebSocketMessageFormatter

    /**
     * Creates an HTTP stream message formatter with custom options.
     */
    fun createHttpStreamFormatter(options: FormatterOptions = FormatterOptions()): HttpStreamMessageFormatter

    /**
     * Creates an SSE stream message formatter with custom options.
     */
    fun createSseStreamFormatter(options: FormatterOptions = FormatterOptions()): SSEStreamMessageFormatter

    /**
     * Creates an HTTP response message formatter with custom options.
     */
    fun createHttpResponseFormatter(options: FormatterOptions = FormatterOptions()): HttpResponseMessageFormatter

    /**
     * Creates a long polling message formatter with custom options.
     */
    fun createLongPollingFormatter(options: FormatterOptions = FormatterOptions()): LongPollingMessageFormatter
}

/**
 * Default implementation of FormatterFactory.
 */
class DefaultFormatterFactory(
    private val serializationService: MessageSerializationService,
) : FormatterFactory {
    /**
     * Creates a WebSocket message formatter with custom options.
     */
    override fun createWebSocketFormatter(options: FormatterOptions): WebSocketMessageFormatter {
        return DefaultWebSocketMessageFormatter(serializationService, options)
    }

    /**
     * Creates an HTTP stream message formatter with custom options.
     */
    override fun createHttpStreamFormatter(options: FormatterOptions): HttpStreamMessageFormatter {
        return SimpleHttpStreamMessageFormatter(serializationService)
    }

    /**
     * Creates an SSE stream message formatter with custom options.
     */
    override fun createSseStreamFormatter(options: FormatterOptions): SSEStreamMessageFormatter {
        return HttpSSEStreamMessageFormatter(serializationService)
    }

    /**
     * Creates an HTTP response message formatter with custom options.
     */
    override fun createHttpResponseFormatter(options: FormatterOptions): HttpResponseMessageFormatter {
        return DefaultHttpResponseMessageFormatter(serializationService)
    }

    /**
     * Creates a long polling message formatter with custom options.
     */
    override fun createLongPollingFormatter(options: FormatterOptions): LongPollingMessageFormatter {
        return DefaultLongPollingMessageFormatter(serializationService)
    }
}
