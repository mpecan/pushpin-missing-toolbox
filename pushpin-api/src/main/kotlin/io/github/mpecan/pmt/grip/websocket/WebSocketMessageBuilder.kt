package io.github.mpecan.pmt.grip.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.grip.GripConstants
import io.github.mpecan.pmt.grip.GripControl

/**
 * Builder for constructing WebSocket messages in GRIP format.
 */
@Suppress("unused")
class WebSocketMessageBuilder {
    private val events = mutableListOf<WebSocketEvent>()
    private val objectMapper = ObjectMapper()

    /**
     * Adds an OPEN event.
     */
    fun open(): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.OPEN))
        return this
    }

    /**
     * Adds a TEXT event with the given content.
     */
    fun text(content: String): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.TEXT, content))
        return this
    }

    /**
     * Adds a TEXT event with a JSON message.
     */
    fun message(content: Any): WebSocketMessageBuilder {
        val json = objectMapper.writeValueAsString(content)
        return text("${GripConstants.WS_MESSAGE_PREFIX}$json")
    }

    /**
     * Adds a TEXT event with a GRIP control message.
     */
    fun control(control: GripControl): WebSocketMessageBuilder {
        val json = objectMapper.writeValueAsString(control)
        return text("${GripConstants.WS_CONTROL_PREFIX}$json")
    }

    /**
     * Adds a subscribe control message.
     */
    fun subscribe(
        channel: String,
        prevId: String? = null,
    ): WebSocketMessageBuilder =
        control(
            io.github.mpecan.pmt.grip
                .GripSubscribeControl(channel, prevId = prevId),
        )

    /**
     * Adds an unsubscribe control message.
     */
    fun unsubscribe(channel: String): WebSocketMessageBuilder =
        control(
            io.github.mpecan.pmt.grip
                .GripUnsubscribeControl(channel),
        )

    /**
     * Adds a keep-alive control message.
     */
    fun keepAlive(
        timeout: Int? = null,
        content: String? = null,
    ): WebSocketMessageBuilder =
        control(
            io.github.mpecan.pmt.grip
                .GripKeepAliveControl(timeout, content),
        )

    /**
     * Adds a detach control message.
     */
    fun detach(): WebSocketMessageBuilder =
        control(
            io.github.mpecan.pmt.grip
                .GripDetachControl(),
        )

    /**
     * Adds a BINARY event with the given content.
     */
    fun binary(content: String): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.BINARY, content))
        return this
    }

    /**
     * Adds a PING event.
     */
    fun ping(): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.PING))
        return this
    }

    /**
     * Adds a PONG event.
     */
    fun pong(): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.PONG))
        return this
    }

    /**
     * Adds a CLOSE event.
     */
    fun close(content: String = ""): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.CLOSE, content))
        return this
    }

    /**
     * Adds a DISCONNECT event.
     */
    fun disconnect(): WebSocketMessageBuilder {
        events.add(WebSocketEvent(WebSocketEventType.DISCONNECT))
        return this
    }

    /**
     * Builds the WebSocket message as a string.
     */
    fun build(): String = WebSocketEventParser.encode(events)

    /**
     * Gets the list of events.
     */
    fun getEvents(): List<WebSocketEvent> = events.toList()
}
