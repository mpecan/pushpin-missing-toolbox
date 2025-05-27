package io.github.mpecan.pmt.client.serialization

import io.github.mpecan.pmt.client.formatter.FormatterFactory
import io.github.mpecan.pmt.client.formatter.HttpResponseMessageFormatter
import io.github.mpecan.pmt.client.formatter.HttpStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.LongPollingMessageFormatter
import io.github.mpecan.pmt.client.formatter.SSEStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.WebSocketMessageFormatter

/**
 * Builder for creating customized MessageSerializer instances.
 * Provides a fluent API for configuring the serializer with specific formatters.
 */
class MessageSerializerBuilder {
    private var webSocketFormatter: WebSocketMessageFormatter? = null
    private var httpSseStreamFormatter: SSEStreamMessageFormatter? = null
    private var httpStreamMessageFormatter: HttpStreamMessageFormatter? = null
    private var httpResponseFormatter: HttpResponseMessageFormatter? = null
    private var longPollingFormatter: LongPollingMessageFormatter? = null
    private var formatterFactory: FormatterFactory? = null

    /**
     * Sets the formatter factory to use for creating any unspecified formatters.
     */
    fun withFormatterFactory(formatterFactory: FormatterFactory): MessageSerializerBuilder {
        this.formatterFactory = formatterFactory
        return this
    }

    /**
     * Sets the WebSocket message formatter.
     */
    fun withWebSocketFormatter(formatter: WebSocketMessageFormatter): MessageSerializerBuilder {
        webSocketFormatter = formatter
        return this
    }

    /**
     * Sets the HTTP SSE stream message formatter.
     */
    fun withHttpSseStreamFormatter(formatter: SSEStreamMessageFormatter): MessageSerializerBuilder {
        httpSseStreamFormatter = formatter
        return this
    }

    /**
     * Sets the HTTP stream message formatter.
     */
    fun withHttpStreamFormatter(formatter: HttpStreamMessageFormatter): MessageSerializerBuilder {
        httpStreamMessageFormatter = formatter
        return this
    }

    /**
     * Sets the HTTP response message formatter.
     */
    fun withHttpResponseFormatter(formatter: HttpResponseMessageFormatter): MessageSerializerBuilder {
        httpResponseFormatter = formatter
        return this
    }

    /**
     * Sets the long-polling message formatter.
     */
    fun withLongPollingFormatter(formatter: LongPollingMessageFormatter): MessageSerializerBuilder {
        longPollingFormatter = formatter
        return this
    }

    /**
     * Builds a MessageSerializer with the configured formatters.
     * If a formatter factory is provided, it will be used to create any unspecified formatters.
     * If a formatter is not specified and no factory is provided, an error will be thrown.
     */
    fun build(): MessageSerializer {
        val factory = formatterFactory

        // If a formatter factory is provided, use it to create any unspecified formatters
        if (factory != null) {
            val wsFormatter = webSocketFormatter ?: factory.createWebSocketFormatter()
            val sseFormatter = httpSseStreamFormatter ?: factory.createSseStreamFormatter()
            val httpStreamFormatter =
                httpStreamMessageFormatter ?: factory.createHttpStreamFormatter()
            val httpResponseFormatter =
                httpResponseFormatter ?: factory.createHttpResponseFormatter()
            val longPollingFormatter = longPollingFormatter ?: factory.createLongPollingFormatter()

            return DefaultMessageSerializer(
                wsFormatter,
                sseFormatter,
                httpStreamFormatter,
                httpResponseFormatter,
                longPollingFormatter,
            )
        }

        // If no formatter factory is provided, all formatters must be specified
        checkNotNull(webSocketFormatter) {
            "WebSocketFormatter must be provided or a FormatterFactory must be set"
        }
        checkNotNull(httpSseStreamFormatter) {
            "SSEStreamFormatter must be provided or a FormatterFactory must be set"
        }
        checkNotNull(httpStreamMessageFormatter) {
            "HttpStreamFormatter must be provided or a FormatterFactory must be set"
        }
        checkNotNull(httpResponseFormatter) {
            "HttpResponseFormatter must be provided or a FormatterFactory must be set"
        }
        checkNotNull(longPollingFormatter) {
            "LongPollingFormatter must be provided or a FormatterFactory must be set"
        }

        return DefaultMessageSerializer(
            webSocketFormatter!!,
            httpSseStreamFormatter!!,
            httpStreamMessageFormatter!!,
            httpResponseFormatter!!,
            longPollingFormatter!!,
        )
    }

    companion object {
        /**
         * Creates a new MessageSerializerBuilder.
         */
        fun builder(): MessageSerializerBuilder = MessageSerializerBuilder()

        /**
         * Creates a default MessageSerializer using the provided FormatterFactory.
         */
        fun defaultSerializer(formatterFactory: FormatterFactory): MessageSerializer {
            return builder()
                .withFormatterFactory(formatterFactory)
                .build()
        }
    }
}
