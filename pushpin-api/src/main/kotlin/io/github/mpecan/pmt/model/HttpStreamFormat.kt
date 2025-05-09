package io.github.mpecan.pmt.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Format for HTTP stream
 * * @property content The text content to send (used when action is "send")
 * @property contentBin The binary content to send, Base64-encoded (used when action is "send")
 * @property action The action to perform: "send" (default), "close", "hint"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HttpStreamFormat(
    val content: String? = null,

    @get:JsonProperty("content-bin")
    val contentBin: String? = null,

    val action: String = "send",
) : PushpinFormat {
    companion object {
        const val ACTION_SEND = "send"
        const val ACTION_CLOSE = "close"
        const val ACTION_HINT = "hint"

        /**
         * Creates an HTTP stream format for sending text data.
         */
        fun send(content: String): HttpStreamFormat {
            return HttpStreamFormat(
                content = content,
                action = ACTION_SEND,
            )
        }

        /**
         * Creates an HTTP stream format for sending binary data.
         */
        fun sendBinary(contentBin: String): HttpStreamFormat {
            return HttpStreamFormat(
                contentBin = contentBin,
                action = ACTION_SEND,
            )
        }

        /**
         * Creates an HTTP stream format for closing the connection.
         */
        fun close(): HttpStreamFormat {
            return HttpStreamFormat(
                action = ACTION_CLOSE,
            )
        }
    }
}
