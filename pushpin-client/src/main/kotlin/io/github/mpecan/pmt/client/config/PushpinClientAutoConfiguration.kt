package io.github.mpecan.pmt.client.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.client.formatter.DefaultFormatterFactory
import io.github.mpecan.pmt.client.formatter.DefaultHttpResponseMessageFormatter
import io.github.mpecan.pmt.client.formatter.DefaultLongPollingMessageFormatter
import io.github.mpecan.pmt.client.formatter.DefaultWebSocketMessageFormatter
import io.github.mpecan.pmt.client.formatter.FormatterFactory
import io.github.mpecan.pmt.client.formatter.FormatterOptions
import io.github.mpecan.pmt.client.formatter.HttpResponseMessageFormatter
import io.github.mpecan.pmt.client.formatter.HttpSSEStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.HttpStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.LongPollingMessageFormatter
import io.github.mpecan.pmt.client.formatter.SSEStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.SimpleHttpStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.WebSocketMessageFormatter
import io.github.mpecan.pmt.client.serialization.JacksonMessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.client.serialization.MessageSerializerBuilder
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for Pushpin client.
 * Provides default implementations of message formatters and serializers.
 */
@AutoConfiguration
@EnableConfigurationProperties(PushpinClientProperties::class)
class PushpinClientAutoConfiguration(
    private val properties: PushpinClientProperties,
) {
    /**
     * Creates a message serialization service bean if none is provided.
     * This service properly handles escaping of special characters in JSON strings.
     */
    @Bean
    @ConditionalOnMissingBean
    fun messageSerializationService(objectMapper: ObjectMapper): MessageSerializationService =
        JacksonMessageSerializationService(objectMapper)

    /**
     * Creates a WebSocket message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun webSocketMessageFormatter(serializationService: MessageSerializationService): WebSocketMessageFormatter {
        // Create formatter options from properties
        val options =
            FormatterOptions()
                .let {
                    if (properties.webSocket.type != null) {
                        it.withOption(
                            DefaultWebSocketMessageFormatter.OPTION_WS_TYPE,
                            properties.webSocket.type,
                        )
                    } else {
                        it
                    }
                }.let {
                    if (properties.webSocket.action != null) {
                        it.withOption(
                            DefaultWebSocketMessageFormatter.OPTION_WS_ACTION,
                            properties.webSocket.action,
                        )
                    } else {
                        it
                    }
                }

        return DefaultWebSocketMessageFormatter(serializationService, options)
    }

    /**
     * Creates an HTTP stream message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpSseStreamMessageFormatter(serializationService: MessageSerializationService): SSEStreamMessageFormatter =
        HttpSSEStreamMessageFormatter(serializationService)

    /**
     * Creates an HTTP response message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpResponseMessageFormatter(serializationService: MessageSerializationService): HttpResponseMessageFormatter =
        DefaultHttpResponseMessageFormatter(serializationService)

    /**
     * Creates an HTTP stream message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpStreamMessageFormatter(serializationService: MessageSerializationService): HttpStreamMessageFormatter =
        SimpleHttpStreamMessageFormatter(serializationService)

    /**
     * Creates a long-polling message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun longPollingMessageFormatter(serializationService: MessageSerializationService): LongPollingMessageFormatter =
        DefaultLongPollingMessageFormatter(serializationService)

    /**
     * Creates a message serializer bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun messageSerializer(formatterFactory: FormatterFactory): MessageSerializer =
        MessageSerializerBuilder.defaultSerializer(formatterFactory)

    /**
     * Creates customized message formatter factories.
     * These factories can be used to create customized formatters.
     */
    @Bean
    fun formatterFactory(serializationService: MessageSerializationService): FormatterFactory =
        DefaultFormatterFactory(serializationService)
}
