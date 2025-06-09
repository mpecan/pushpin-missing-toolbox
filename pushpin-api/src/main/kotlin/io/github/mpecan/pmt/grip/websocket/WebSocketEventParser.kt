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
            val parsedLine = parseEventLine(body, index) ?: break

            val eventType = parseEventType(parsedLine.eventTypeStr)
            if (eventType == null) {
                index = parsedLine.nextIndex
                continue
            }

            val contentSize = parseContentSize(parsedLine.parts)
            if (contentSize == -1) break

            val contentResult = extractContent(body, parsedLine.nextIndex, contentSize)
            if (contentResult == null) break

            events.add(WebSocketEvent(eventType, contentResult.content))
            index = contentResult.nextIndex
        }

        return events
    }

    private data class ParsedLine(
        val eventTypeStr: String,
        val parts: List<String>,
        val nextIndex: Int,
    )

    private data class ContentResult(
        val content: String,
        val nextIndex: Int,
    )

    private fun parseEventLine(
        body: String,
        startIndex: Int,
    ): ParsedLine? {
        val lineEnd = body.indexOf("\r\n", startIndex)
        if (lineEnd == -1) return null

        val line = body.substring(startIndex, lineEnd)
        val parts = line.split(" ", limit = 2)
        return ParsedLine(parts[0], parts, lineEnd + 2)
    }

    private fun parseEventType(eventTypeStr: String): WebSocketEventType? = WebSocketEventType.fromString(eventTypeStr)

    private fun parseContentSize(parts: List<String>): Int {
        if (parts.size <= 1) return 0

        return try {
            parts[1].toInt(16)
        } catch (e: NumberFormatException) {
            -1
        }
    }

    private fun extractContent(
        body: String,
        startIndex: Int,
        contentSize: Int,
    ): ContentResult? {
        if (contentSize == 0) {
            return ContentResult("", startIndex)
        }

        val contentEnd = startIndex + contentSize
        if (contentEnd > body.length) return null

        val content = body.substring(startIndex, contentEnd)
        return ContentResult(content, contentEnd + 2)
    }

    /**
     * Encodes multiple WebSocket events into a single string.
     *
     * @param events The events to encode
     * @return The encoded events as a string
     */
    fun encode(events: List<WebSocketEvent>): String = events.joinToString("") { it.encode() }
}
