package io.github.mpecan.pmt.client.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.client.formatter.*
import io.github.mpecan.pmt.client.serialization.DefaultMessageSerializer
import io.github.mpecan.pmt.client.serialization.JacksonMessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for Pushpin client.
 * Provides default implementations of message formatters and serializers.
 */
@AutoConfiguration
class PushpinClientAutoConfiguration {

    /**
     * Creates a message serialization service bean if none is provided.
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
        return DefaultWebSocketMessageFormatter(serializationService)
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
    fun messageSerializer(
        webSocketFormatter: WebSocketMessageFormatter,
        httpSseStreamFormatter: SSEStreamMessageFormatter,
        httpStreamMessageFormatter: HttpStreamMessageFormatter,
        httpResponseFormatter: HttpResponseMessageFormatter,
        longPollingFormatter: LongPollingMessageFormatter
    ): MessageSerializer {
        return DefaultMessageSerializer(
            webSocketFormatter,
            httpSseStreamFormatter,
            httpStreamMessageFormatter,
            httpResponseFormatter,
            longPollingFormatter
        )
    }
}
