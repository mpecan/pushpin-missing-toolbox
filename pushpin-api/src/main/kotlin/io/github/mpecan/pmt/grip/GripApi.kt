package io.github.mpecan.pmt.grip

import io.github.mpecan.pmt.grip.auth.GripAuthHelper
import io.github.mpecan.pmt.grip.websocket.WebSocketEvent
import io.github.mpecan.pmt.grip.websocket.WebSocketEventParser
import io.github.mpecan.pmt.grip.websocket.WebSocketMessageBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/**
 * Main API facade for working with the GRIP protocol.
 * * This class provides convenient methods for implementing GRIP (Generic Realtime * Intermediary Protocol) in your Spring Boot applications with Pushpin.
 * * ## Basic Usage
 * * ### HTTP Long-Polling
 * ```kotlin
 * return GripApi.longPollingResponse("channel-name", timeout = 30)
 *     .body(responseData)
 * ```
 * * ### HTTP Streaming
 * ```kotlin
 * return GripApi.streamingResponse("channel-name")
 *     .body(fluxStream)
 * ```
 * * ### Server-Sent Events
 * ```kotlin
 * return GripApi.sseResponse("channel-name")
 *     .body(eventStream)
 * ```
 * * ### WebSocket-over-HTTP
 * ```kotlin
 * val events = GripApi.parseWebSocketEvents(requestBody)
 * val response = GripApi.websocket()
 *     .open()
 *     .subscribe("channel")
 *     .build()
 * return GripApi.websocketResponse(secWebSocketAccept)
 *     .body(response)
 * ```
 * * @see GripHeaderBuilder for advanced header configuration
 * @see WebSocketMessageBuilder for WebSocket message construction
 * @see GripAuthHelper for authentication utilities
 */
@Suppress("unused")
object GripApi {
    private const val META_PREFIX_LENGTH = GripConstants.HEADER_META_PREFIX.length

    /**
     * Creates a new GRIP header builder.
     */
    fun headers(): GripHeaderBuilder = GripHeaderBuilder()

    /**
     * Creates a new WebSocket message builder.
     */
    fun websocket(): WebSocketMessageBuilder = WebSocketMessageBuilder()

    /**
     * Parses WebSocket events from a request body.
     */
    fun parseWebSocketEvents(body: String): List<WebSocketEvent> = WebSocketEventParser.parse(body)

    /**
     * Creates a GRIP signature for authentication.
     */
    fun createSignature(
        issuer: String,
        key: String,
        expiresIn: Long? = null,
    ): String = GripAuthHelper.createGripSignature(issuer, key, expiresIn)

    /**
     * Validates a GRIP signature.
     */
    fun validateSignature(
        token: String,
        key: String,
    ): Boolean = GripAuthHelper.validateGripSignature(token, key)

    /**
     * Creates a ResponseEntity builder for HTTP long-polling with GRIP headers.
     */
    fun <T> longPollingResponse(
        channel: String,
        timeout: Int = GripConstants.DEFAULT_TIMEOUT,
    ): ResponseEntity.BodyBuilder =
        ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_RESPONSE)
            .header(GripConstants.HEADER_GRIP_CHANNEL, channel)
            .header(GripConstants.HEADER_GRIP_TIMEOUT, timeout.toString())

    /**
     * Creates a ResponseEntity builder for HTTP streaming with GRIP headers.
     */
    fun streamingResponse(channel: String): ResponseEntity.BodyBuilder =
        ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .header(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_STREAM)
            .header(GripConstants.HEADER_GRIP_CHANNEL, channel)

    /**
     * Creates a ResponseEntity builder for Server-Sent Events with GRIP headers.
     */
    fun sseResponse(channel: String): ResponseEntity.BodyBuilder =
        ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_STREAM)
            .header(GripConstants.HEADER_GRIP_CHANNEL, channel)

    /**
     * Creates a ResponseEntity builder for WebSocket-over-HTTP with GRIP headers.
     */
    fun websocketResponse(
        secWebSocketAccept: String,
        keepAliveInterval: Int = GripConstants.DEFAULT_KEEP_ALIVE_INTERVAL,
    ): ResponseEntity.BodyBuilder =
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(GripConstants.CONTENT_TYPE_WEBSOCKET_EVENTS))
            .header(GripConstants.HEADER_SEC_WEBSOCKET_EXTENSIONS, GripConstants.WEBSOCKET_EXTENSION_GRIP)
            .header(GripConstants.HEADER_SEC_WEBSOCKET_ACCEPT, secWebSocketAccept)
            .header(GripConstants.HEADER_KEEP_ALIVE_INTERVAL, keepAliveInterval.toString())

    /**
     * Extracts meta headers from request headers.
     * * @param headers The request headers
     * @return Map of meta headers without the "Meta-" prefix
     */
    @Suppress("kotlin:S6524")
    fun extractMetaHeaders(headers: HttpHeaders): Map<String, String> =
        headers.entries
            .asSequence()
            .filter { (key, _) -> key.startsWith(GripConstants.HEADER_META_PREFIX, ignoreCase = true) }
            .associate { (key, values) ->
                // Remove the "Meta-" prefix case-insensitively
                key.substring(META_PREFIX_LENGTH) to (values.firstOrNull() ?: "")
            }

    /**
     * Applies meta headers to a response.
     * * @param builder The response builder
     * @param metaHeaders The meta headers to apply
     * @return The response builder with meta headers applied
     */
    @Suppress("UastIncorrectHttpHeaderInspection", "InjectedReferences")
    fun applyMetaHeaders(
        builder: ResponseEntity.BodyBuilder,
        metaHeaders: Map<String, String>,
    ): ResponseEntity.BodyBuilder {
        metaHeaders.forEach { (key, value) ->
            builder.header("${GripConstants.HEADER_SET_META_PREFIX}$key", value)
        }
        return builder
    }
}
