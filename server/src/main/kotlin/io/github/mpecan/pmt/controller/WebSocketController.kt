package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.grip.GripApi
import io.github.mpecan.pmt.grip.websocket.WebSocketEventType
import io.github.mpecan.pmt.grip.websocket.WebSocketMessageBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for WebSocket-over-HTTP communication with PushPin.
 *
 * This controller implements the WebSocket-over-HTTP protocol as described in the PushPin documentation.
 * It handles WebSocket events via HTTP requests and responses, encoding them in a format similar to
 * HTTP chunked transfer encoding.
 */
@RestController
@RequestMapping("/api/ws")
class WebSocketController {

    /**
     * Handles WebSocket-over-HTTP requests from Pushpin.
     * This endpoint processes WebSocket events encoded in the request body and
     * responds with appropriate WebSocket events.
     *
     * @param connectionId The unique identifier for the client connection
     * @param contentType The content type of the request
     * @param headers All request headers
     * @param body The request body containing encoded WebSocket events
     * @return A response with encoded WebSocket events
     */
    @PostMapping("/{channel}")
    fun handleWebSocketOverHttp(
        @PathVariable channel: String,
        @RequestHeader("Connection-Id", required = true) connectionId: String,
        @RequestHeader(
            "Content-Type",
            required = false,
            defaultValue = "application/websocket-events",
        ) contentType: String,
        @RequestHeader("Sec-WebSocket-Key", required = true) secWebSocketAccept: String,
        @RequestHeader headers: HttpHeaders,
        @RequestBody body: String,
    ): ResponseEntity<String> {
        // Process the incoming WebSocket events using GRIP API
        val events = GripApi.parseWebSocketEvents(body)

        // Build response using GRIP API
        val messageBuilder = WebSocketMessageBuilder()

        // Check if this is an initial connection (OPEN event)
        if (events.any { it.type == WebSocketEventType.OPEN }) {
            // Build the response using the new API
            messageBuilder
                .open()
                .subscribe(channel)
                .keepAlive(timeout = 30, content = "{}")
                .message(
                    mapOf(
                        "success" to true,
                        "message" to "Subscribed to channel: $channel",
                    ),
                )
        } else {
            // Process other event types
            for (event in events) {
                when (event.type) {
                    WebSocketEventType.TEXT -> {
                        // Echo the text message back
                        messageBuilder.text(event.content)
                    }

                    WebSocketEventType.BINARY -> {
                        // Echo the binary message back for demonstration
                        messageBuilder.binary(event.content)
                    }

                    WebSocketEventType.PING -> {
                        // Respond with a PONG
                        messageBuilder.pong()
                    }

                    WebSocketEventType.PONG -> {
                        // Received a PONG, typically no response needed
                    }

                    WebSocketEventType.CLOSE -> {
                        // Acknowledge the close
                        messageBuilder.close(event.content)
                    }

                    WebSocketEventType.DISCONNECT -> {
                        // Connection is already closed or doesn't exist
                        messageBuilder.disconnect()
                    }

                    WebSocketEventType.OPEN -> {
                        // OPEN was already handled above
                    }
                }
            }
        }

        // Extract meta headers using GRIP API
        val metaHeaders = GripApi.extractMetaHeaders(headers)

        // Build the response using GRIP API
        var responseBuilder = GripApi.websocketResponse(secWebSocketAccept)

        // Apply meta headers using GRIP API
        responseBuilder = GripApi.applyMetaHeaders(responseBuilder, metaHeaders)

        return responseBuilder.body(messageBuilder.build())
    }
}
