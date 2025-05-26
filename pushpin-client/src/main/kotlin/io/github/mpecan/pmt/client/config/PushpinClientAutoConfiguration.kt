package io.github.mpecan.pmt.client.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.client.formatter.*
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
    fun messageSerializationService(objectMapper: ObjectMapper): MessageSerializationService {
        return JacksonMessageSerializationService(objectMapper)
    }

    /**
     * Creates a WebSocket message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun webSocketMessageFormatter(serializationService: MessageSerializationService): WebSocketMessageFormatter {
        // Create formatter options from properties
        val options = FormatterOptions()
            .let {
                if (properties.webSocket.type != null) {
                    it.withOption(
                        DefaultWebSocketMessageFormatter.OPTION_WS_TYPE,
                        properties.webSocket.type,
                    )
                } else {
                    it
                }
            }
            .let {
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
    fun httpSseStreamMessageFormatter(serializationService: MessageSerializationService): SSEStreamMessageFormatter {
        return HttpSSEStreamMessageFormatter(serializationService)
    }

    /**
     * Creates an HTTP response message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpResponseMessageFormatter(serializationService: MessageSerializationService): HttpResponseMessageFormatter {
        return DefaultHttpResponseMessageFormatter(serializationService)
    }

    /**
     * Creates an HTTP stream message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpStreamMessageFormatter(serializationService: MessageSerializationService): HttpStreamMessageFormatter {
        return SimpleHttpStreamMessageFormatter(serializationService)
    }

    /**
     * Creates a long-polling message formatter bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun longPollingMessageFormatter(serializationService: MessageSerializationService): LongPollingMessageFormatter {
        return DefaultLongPollingMessageFormatter(serializationService)
    }

    /**
     * Creates a message serializer bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun messageSerializer(formatterFactory: FormatterFactory): MessageSerializer {
        return MessageSerializerBuilder.defaultSerializer(formatterFactory)
    }

    /**
     * Creates customized message formatter factories.
     * These factories can be used to create customized formatters.
     */
    @Bean
    fun formatterFactory(serializationService: MessageSerializationService): FormatterFactory {
        return DefaultFormatterFactory(serializationService)
    }
}
