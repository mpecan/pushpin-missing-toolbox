package io.github.mpecan.pmt.grip.websocket

/**
 * Parser for WebSocket events in GRIP format.
 */
object WebSocketEventParser {
    /**
     * Parses WebSocket events from a GRIP WebSocket-over-HTTP request body.
     *
     * @param body The request body containing encoded WebSocket events
     * @return A list of parsed WebSocket events
     */
    fun parse(body: String): List<WebSocketEvent> {
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

            val eventTypeStr = parts[0]
            val eventType = WebSocketEventType.fromString(eventTypeStr)
            if (eventType == null) {
                // Skip to the next line if we encounter an unknown event type
                index = lineEnd + 2
                continue
            }

            val contentSize =
                if (parts.size > 1) {
                    try {
                        parts[1].toInt(16)
                    } catch (e: NumberFormatException) {
                        // Invalid size, skip this event
                        break
                    }
                } else {
                    0
                }

            index = lineEnd + 2 // Skip \r\n

            // Extract content if present
            val content =
                if (contentSize > 0) {
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
     * Encodes multiple WebSocket events into a single string.
     *
     * @param events The events to encode
     * @return The encoded events as a string
     */
    fun encode(events: List<WebSocketEvent>): String = events.joinToString("") { it.encode() }
}
