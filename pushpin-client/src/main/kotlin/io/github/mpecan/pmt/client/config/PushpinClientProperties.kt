package io.github.mpecan.pmt.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * Configuration properties for Pushpin client.
 */
@ConfigurationProperties(prefix = "pushpin.client")
data class PushpinClientProperties(
    /**
     * WebSocket formatter configuration.
     */
    @NestedConfigurationProperty
    val webSocket: WebSocketProperties = WebSocketProperties(),
    /**
     * HTTP Stream formatter configuration.
     */
    @NestedConfigurationProperty
    val httpStream: HttpStreamProperties = HttpStreamProperties(),
    /**
     * HTTP Response formatter configuration.
     */
    @NestedConfigurationProperty
    val httpResponse: HttpResponseProperties = HttpResponseProperties(),
    /**
     * SSE formatter configuration.
     */
    @NestedConfigurationProperty
    val sse: SSEProperties = SSEProperties(),
    /**
     * Long polling formatter configuration.
     */
    @NestedConfigurationProperty
    val longPolling: LongPollingProperties = LongPollingProperties(),
)

/**
 * WebSocket formatter properties.
 */
data class WebSocketProperties(
    /**
     * WebSocket message type.
     */
    val type: String? = null,
    /**
     * WebSocket message action.
     */
    val action: String? = null,
)

/**
 * HTTP Stream formatter properties.
 */
data class HttpStreamProperties(
    /**
     * HTTP Stream action.
     */
    val action: String? = null,
)

/**
 * HTTP Response formatter properties.
 */
data class HttpResponseProperties(
    /**
     * HTTP Response code.
     */
    val code: Int? = null,
    /**
     * HTTP Response reason.
     */
    val reason: String? = null,
    /**
     * HTTP Response headers.
     */
    val headers: Map<String, String> = emptyMap(),
)

/**
 * SSE formatter properties.
 */
data class SSEProperties(
    /**
     * SSE event name.
     */
    val eventName: String? = null,
)

/**
 * Long polling formatter properties.
 */
data class LongPollingProperties(
    /**
     * Long polling code.
     */
    val code: Int? = null,
    /**
     * Long polling reason.
     */
    val reason: String? = null,
    /**
     * Long polling headers.
     */
    val headers: Map<String, String> = emptyMap(),
)
