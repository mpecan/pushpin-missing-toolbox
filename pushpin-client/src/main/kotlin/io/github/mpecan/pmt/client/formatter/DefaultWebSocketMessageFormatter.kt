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
        
        // Default values
        const val DEFAULT_WS_TYPE = "text"
        const val DEFAULT_WS_ACTION = "send"
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
        
        // Check for type and action in message metadata (overrides options)
        val metaType = message.meta?.get("ws.type") as? String
        val metaAction = message.meta?.get("ws.action") as? String
        
        return WebSocketFormat(
            content = serializationService.serialize(message),
            type = metaType ?: type,
            action = metaAction ?: action
        )
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
}