package io.github.mpecan.pmt.controller

import org.springframework.http.MediaType
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
        @RequestHeader headers: Map<String, String>,
        @RequestBody body: String,
    ): ResponseEntity<String> {
        // Process the incoming WebSocket events
        val events = parseWebSocketEvents(body)

        // Prepare the response
        val responseBuilder = StringBuilder()

        // Check if this is an initial connection (OPEN event)
        if (events.any { it.type == "OPEN" }) {
            // Acknowledge the connection
            responseBuilder.append("OPEN\r\n")

            // Subscribe to the channel
            val subscribeMessage = """c:{"type":"subscribe","channel":"$channel"}"""
            val encodedMessage = encodeTextEvent(subscribeMessage)
            responseBuilder.append(encodedMessage)
            val keepAliveMessage = """c:{"type": "keep-alive", "content": "{}", "timeout": 30}"""
            responseBuilder.append(encodeTextEvent(keepAliveMessage))

            val returnMessage = """m:{"success": true, "message": "Subscribed to channel: $channel"}"""
            responseBuilder.append(encodeTextEvent(returnMessage))
        } else {
            // Process other event types
            for (event in events) {
                when (event.type) {
                    "TEXT" -> {
                        // Echo the text message back
                        responseBuilder.append(encodeTextEvent(event.content))
                    }

                    "BINARY" -> {
                        // Echo the binary message back for demonstration
                        responseBuilder.append(encodeBinaryEvent(event.content))
                    }

                    "PING" -> {
                        // Respond with a PONG
                        responseBuilder.append("PONG\r\n")
                    }

                    "CLOSE" -> {
                        // Acknowledge the close
                        if (event.content.isNotEmpty()) {
                            responseBuilder.append("CLOSE ${event.content.length.toString(16)}\r\n${event.content}\r\n")
                        } else {
                            responseBuilder.append("CLOSE\r\n")
                        }
                    }

                    "DISCONNECT" -> {
                        // Connection is already closed or doesn't exist
                        responseBuilder.append("DISCONNECT\r\n")
                    }
                }
            }
        }

        // Extract any Meta headers to include in the response
        val metaHeaders = headers.filterKeys { it.startsWith("Meta-") }

        // Build the response
        val responseEntity = ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/websocket-events"))
            .header("Sec-WebSocket-Extensions", "grip")
            .header("Sec-WebSocket-Accept", secWebSocketAccept)

        // Add Keep-Alive-Interval if needed
        responseEntity.header("Keep-Alive-Interval", "120")

        // Add any Set-Meta headers
        metaHeaders.forEach { (key, value) ->
            val metaKey = key.removePrefix("Meta-")
            responseEntity.header("Set-Meta-$metaKey", value)
        }

        return responseEntity.body(responseBuilder.toString())
    }

    /**
     * Parses WebSocket events from the request body.
     *
     * @param body The request body containing encoded WebSocket events
     * @return A list of WebSocketEvent objects
     */
    private fun parseWebSocketEvents(body: String): List<WebSocketEvent> {
        if (body.isEmpty()) {
            return emptyList()
        }

        val events = mutableListOf<WebSocketEvent>()
        var index = 0

        while (index < body.length) {
            // Find the event type and size
            val lineEnd = body.indexOf("\r\n", index)
            if (lineEnd == -1) break

            val line = body.substring(index, lineEnd)
            val parts = line.split(" ", limit = 2)

            val eventType = parts[0]
            val contentSize = if (parts.size > 1) parts[1].toInt(16) else 0

            index = lineEnd + 2 // Skip \r\n

            // Extract content if present
            val content = if (contentSize > 0) {
                val contentEnd = index + contentSize
                if (contentEnd <= body.length) {
                    val contentStr = body.substring(index, contentEnd)
                    index = contentEnd + 2 // Skip \r\n after content
                    contentStr
                } else {
                    // Malformed event, skip it
                    break
                }
            } else {
                ""
            }

            events.add(WebSocketEvent(eventType, content))
        }

        return events
    }

    /**
     * Encodes a text message as a WebSocket TEXT event.
     *
     * @param message The text message to encode
     * @return The encoded WebSocket event
     */
    private fun encodeTextEvent(message: String): String {
        val hexSize = message.length.toString(16)
        return "TEXT $hexSize\r\n$message\r\n"
    }

    /**
     * Encodes a binary message as a WebSocket BINARY event.
     *
     * @param content The binary content to encode
     * @return The encoded WebSocket event
     */
    private fun encodeBinaryEvent(content: String): String {
        val hexSize = content.length.toString(16)
        return "BINARY $hexSize\r\n$content\r\n"
    }

    /**
     * Represents a WebSocket event with a type and optional content.
     */
    data class WebSocketEvent(val type: String, val content: String)
}
