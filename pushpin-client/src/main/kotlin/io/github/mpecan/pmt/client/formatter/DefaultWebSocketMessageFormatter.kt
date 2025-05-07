package io.github.mpecan.pmt.client.formatter

import io.github.mpecan.pmt.client.exception.MessageFormattingException
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.model.WebSocketFormat

/**
 * Default implementation of WebSocketMessageFormatter.
 * Supports customization through FormatterOptions.
 */
class DefaultWebSocketMessageFormatter(
    serializationService: MessageSerializationService,
    options: FormatterOptions = FormatterOptions()
) : AbstractMessageFormatter<WebSocketFormat>(serializationService, options), WebSocketMessageFormatter {
    
    /**
     * Additional WebSocket-specific options.
     */
    companion object {
        const val OPTION_WS_TYPE = "ws.type"
        const val OPTION_WS_ACTION = "ws.action"
        const val OPTION_WS_CLOSE_CODE = "ws.close.code"
        
        // Default values
        const val DEFAULT_WS_TYPE = WebSocketFormat.TYPE_TEXT
        const val DEFAULT_WS_ACTION = WebSocketFormat.ACTION_SEND
    }
    
    /**
     * Formats a message for WebSocket transport.
     *
     * @param message The message to format
     * @return The formatted WebSocket message
     * @throws MessageFormattingException If there is an error during formatting
     */
    @Throws(MessageFormattingException::class)
    override fun format(message: Message): WebSocketFormat {
        return formatWithHooks(message)
    }
    
    /**
     * Performs the actual formatting of the message.
     *
     * @param message The message to format
     * @return The formatted WebSocket message
     */
    override fun doFormat(message: Message): WebSocketFormat {
        // Get type and action from options or use defaults
        val type = options.additionalOptions[OPTION_WS_TYPE] as? String ?: DEFAULT_WS_TYPE
        val action = options.additionalOptions[OPTION_WS_ACTION] as? String ?: DEFAULT_WS_ACTION
        val closeCode = options.additionalOptions[OPTION_WS_CLOSE_CODE] as? Int
        
        // Check for type and action in message metadata (overrides options)
        val metaType = message.meta?.get("ws.type") as? String
        val metaAction = message.meta?.get("ws.action") as? String
        val metaCloseCode = message.meta?.get("ws.close.code") as? Int
        
        val finalAction = metaAction ?: action
        
        return when (finalAction) {
            WebSocketFormat.ACTION_CLOSE -> {
                WebSocketFormat.close(metaCloseCode ?: closeCode)
            }
            WebSocketFormat.ACTION_HINT -> {
                WebSocketFormat(action = WebSocketFormat.ACTION_HINT)
            }
            else -> {
                val finalType = metaType ?: type
                if (finalType == WebSocketFormat.TYPE_BINARY) {
                    WebSocketFormat.sendBinary(serializationService.serialize(message))
                } else {
                    WebSocketFormat.sendText(serializationService.serialize(message))
                }
            }
        }
    }
    
    /**
     * Creates a new formatter with custom WebSocket type.
     */
    fun withType(type: String): DefaultWebSocketMessageFormatter {
        return DefaultWebSocketMessageFormatter(
            serializationService,
            options.withOption(OPTION_WS_TYPE, type)
        )
    }
    
    /**
     * Creates a new formatter with custom WebSocket action.
     */
    fun withAction(action: String): DefaultWebSocketMessageFormatter {
        return DefaultWebSocketMessageFormatter(
            serializationService,
            options.withOption(OPTION_WS_ACTION, action)
        )
    }
    
    /**
     * Creates a new formatter for closing WebSocket connections.
     */
    fun withCloseAction(closeCode: Int? = null): DefaultWebSocketMessageFormatter {
        val opts = options
            .withOption(OPTION_WS_ACTION, WebSocketFormat.ACTION_CLOSE)
        
        return if (closeCode != null) {
            DefaultWebSocketMessageFormatter(
                serializationService,
                opts.withOption(OPTION_WS_CLOSE_CODE, closeCode)
            )
        } else {
            DefaultWebSocketMessageFormatter(
                serializationService,
                opts
            )
        }
    }
}