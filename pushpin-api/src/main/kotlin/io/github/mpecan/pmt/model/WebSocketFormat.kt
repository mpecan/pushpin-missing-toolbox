package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Format for WebSocket messages
 * * @property content The text content to send (used when action is "send")
 * @property contentBin The binary content to send, Base64-encoded (used when action is "send")
 * @property type The WebSocket message type: "text" or "binary" (used when action is "send")
 * @property action The action to perform: "send" (default), "close", "hint"
 * @property code The WebSocket close code (used when action is "close")
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WebSocketFormat(
    val content: String? = null,
    @get:JsonProperty("content-bin")
    val contentBin: String? = null,
    val type: String = "text",
    val action: String = "send",
    val code: Int? = null,
) : PushpinFormat {
    companion object {
        const val ACTION_SEND = "send"
        const val ACTION_CLOSE = "close"
        const val ACTION_HINT = "hint"

        const val TYPE_TEXT = "text"
        const val TYPE_BINARY = "binary"

        /**
         * Creates a WebSocket format for sending text data.
         */
        fun sendText(content: String): WebSocketFormat =
            WebSocketFormat(
                content = content,
                type = TYPE_TEXT,
                action = ACTION_SEND,
            )

        /**
         * Creates a WebSocket format for sending binary data.
         */
        fun sendBinary(contentBin: String): WebSocketFormat =
            WebSocketFormat(
                contentBin = contentBin,
                type = TYPE_BINARY,
                action = ACTION_SEND,
            )

        /**
         * Creates a WebSocket format for closing the connection.
         */
        fun close(code: Int? = null): WebSocketFormat =
            WebSocketFormat(
                action = ACTION_CLOSE,
                code = code,
            )
    }
}
