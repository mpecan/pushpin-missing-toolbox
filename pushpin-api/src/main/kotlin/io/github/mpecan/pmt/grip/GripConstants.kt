package io.github.mpecan.pmt.grip

/**
 * Constants for the GRIP (Generic Realtime Intermediary Protocol) protocol.
 */
@Suppress("unused")
object GripConstants {
    // GRIP Headers
    const val HEADER_GRIP_SIG = "Grip-Sig"
    const val HEADER_GRIP_HOLD = "Grip-Hold"
    const val HEADER_GRIP_CHANNEL = "Grip-Channel"
    const val HEADER_GRIP_TIMEOUT = "Grip-Timeout"
    const val HEADER_GRIP_KEEP_ALIVE = "Grip-Keep-Alive"
    const val HEADER_GRIP_KEEP_ALIVE_FORMAT = "Grip-Keep-Alive-Format"
    const val HEADER_GRIP_KEEP_ALIVE_TIMEOUT = "Grip-Keep-Alive-Timeout"
    const val HEADER_GRIP_SET_HOLD = "Grip-Set-Hold"
    const val HEADER_GRIP_SET_CHANNEL = "Grip-Set-Channel"
    const val HEADER_GRIP_LAST = "Grip-Last"
    const val HEADER_GRIP_PREVIOUS_ID = "Grip-Previous-Id"

    // WebSocket Headers
    const val HEADER_CONNECTION_ID = "Connection-Id"
    const val HEADER_SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key"
    const val HEADER_SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept"
    const val HEADER_SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions"
    const val HEADER_KEEP_ALIVE_INTERVAL = "Keep-Alive-Interval"
    const val HEADER_CONTENT_TYPE = "Content-Type"

    // Meta Headers
    const val HEADER_META_PREFIX = "Meta-"
    const val HEADER_SET_META_PREFIX = "Set-Meta-"

    // Hold Modes
    const val HOLD_MODE_RESPONSE = "response"
    const val HOLD_MODE_STREAM = "stream"

    // WebSocket Extensions
    const val WEBSOCKET_EXTENSION_GRIP = "grip"

    // Content Types
    const val CONTENT_TYPE_WEBSOCKET_EVENTS = "application/websocket-events"

    // WebSocket Event Types
    const val WS_EVENT_OPEN = "OPEN"
    const val WS_EVENT_TEXT = "TEXT"
    const val WS_EVENT_BINARY = "BINARY"
    const val WS_EVENT_PING = "PING"
    const val WS_EVENT_PONG = "PONG"
    const val WS_EVENT_CLOSE = "CLOSE"
    const val WS_EVENT_DISCONNECT = "DISCONNECT"

    // WebSocket Control Prefixes
    const val WS_CONTROL_PREFIX = "c:"
    const val WS_MESSAGE_PREFIX = "m:"

    // Default Values
    const val DEFAULT_TIMEOUT = 20
    const val DEFAULT_KEEP_ALIVE_INTERVAL = 120
    const val DEFAULT_KEEP_ALIVE_TIMEOUT = 30
}
