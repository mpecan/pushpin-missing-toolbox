package io.github.mpecan.pmt.grip.websocket

/**
 * Represents a WebSocket event in the GRIP protocol.
 */
data class WebSocketEvent(
    val type: WebSocketEventType,
    val content: String = "",
) {
    /**
     * Encodes the event into GRIP WebSocket-over-HTTP format.
     */
    fun encode(): String {
        return when (type) {
            WebSocketEventType.OPEN,
            WebSocketEventType.PING,
            WebSocketEventType.PONG,
            WebSocketEventType.DISCONNECT,
            -> {
                // These events don't have content
                "${type.value}\r\n"
            }
            WebSocketEventType.TEXT,
            WebSocketEventType.BINARY,
            WebSocketEventType.CLOSE,
            -> {
                if (content.isEmpty()) {
                    "${type.value}\r\n"
                } else {
                    val hexSize = content.length.toString(16)
                    "${type.value} $hexSize\r\n$content\r\n"
                }
            }
        }
    }
}

/**
 * WebSocket event types supported by GRIP.
 */
enum class WebSocketEventType(val value: String) {
    OPEN("OPEN"),
    TEXT("TEXT"),
    BINARY("BINARY"),
    PING("PING"),
    PONG("PONG"),
    CLOSE("CLOSE"),
    DISCONNECT("DISCONNECT"),
    ;

    companion object {
        fun fromString(value: String): WebSocketEventType? {
            return entries.find { it.value == value }
        }
    }
}
