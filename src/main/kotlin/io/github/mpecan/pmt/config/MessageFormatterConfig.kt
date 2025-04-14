package io.github.mpecan.pmt.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.formatter.*
import io.github.mpecan.pmt.serialization.JacksonMessageSerializationService
import io.github.mpecan.pmt.serialization.MessageSerializationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for message formatters.
 */
@Configuration
class MessageFormatterConfig {
    
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
}