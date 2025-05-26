package io.github.mpecan.pmt.grip

import org.springframework.http.ResponseEntity

/**
 * Builder for constructing HTTP responses with GRIP headers.
 */
class GripHeaderBuilder {
    private val headers = mutableMapOf<String, String>()
    private var holdMode: String? = null
    private val channels = mutableListOf<String>()
    private var timeout: Int? = null
    private var keepAlive: String? = null
    private var keepAliveFormat: String? = null
    private var keepAliveTimeout: Int? = null
    private var gripSig: String? = null
    private var lastEventId: String? = null
    private var previousId: String? = null
    
    /**
     * Sets the GRIP hold mode (response or stream).
     */
    fun hold(mode: String): GripHeaderBuilder {
        holdMode = mode
        return this
    }
    
    /**
     * Sets the GRIP hold mode to response.
     */
    fun holdResponse(): GripHeaderBuilder {
        return hold(GripConstants.HOLD_MODE_RESPONSE)
    }
    
    /**
     * Sets the GRIP hold mode to stream.
     */
    fun holdStream(): GripHeaderBuilder {
        return hold(GripConstants.HOLD_MODE_STREAM)
    }
    
    /**
     * Adds a channel to subscribe to.
     */
    fun channel(channel: String): GripHeaderBuilder {
        channels.add(channel)
        return this
    }
    
    /**
     * Adds multiple channels to subscribe to.
     */
    fun channels(vararg channels: String): GripHeaderBuilder {
        this.channels.addAll(channels)
        return this
    }
    
    /**
     * Sets the connection timeout in seconds.
     */
    fun timeout(seconds: Int): GripHeaderBuilder {
        timeout = seconds
        return this
    }
    
    /**
     * Sets the keep-alive message content.
     */
    fun keepAlive(content: String): GripHeaderBuilder {
        keepAlive = content
        return this
    }
    
    /**
     * Sets the keep-alive message format.
     */
    fun keepAliveFormat(format: String): GripHeaderBuilder {
        keepAliveFormat = format
        return this
    }
    
    /**
     * Sets the keep-alive timeout in seconds.
     */
    fun keepAliveTimeout(seconds: Int): GripHeaderBuilder {
        keepAliveTimeout = seconds
        return this
    }
    
    /**
     * Sets the GRIP signature for authentication.
     */
    fun gripSig(signature: String): GripHeaderBuilder {
        gripSig = signature
        return this
    }
    
    /**
     * Sets the last event ID.
     */
    fun lastEventId(id: String): GripHeaderBuilder {
        lastEventId = id
        return this
    }
    
    /**
     * Sets the previous ID for message sequencing.
     */
    fun previousId(id: String): GripHeaderBuilder {
        previousId = id
        return this
    }
    
    /**
     * Adds a custom header.
     */
    fun header(name: String, value: String): GripHeaderBuilder {
        headers[name] = value
        return this
    }
    
    /**
     * Builds the headers map.
     */
    fun build(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        holdMode?.let { result[GripConstants.HEADER_GRIP_HOLD] = it }
        
        if (channels.isNotEmpty()) {
            result[GripConstants.HEADER_GRIP_CHANNEL] = channels.joinToString(", ")
        }
        
        timeout?.let { result[GripConstants.HEADER_GRIP_TIMEOUT] = it.toString() }
        keepAlive?.let { result[GripConstants.HEADER_GRIP_KEEP_ALIVE] = it }
        keepAliveFormat?.let { result[GripConstants.HEADER_GRIP_KEEP_ALIVE_FORMAT] = it }
        keepAliveTimeout?.let { result[GripConstants.HEADER_GRIP_KEEP_ALIVE_TIMEOUT] = it.toString() }
        gripSig?.let { result[GripConstants.HEADER_GRIP_SIG] = it }
        lastEventId?.let { result[GripConstants.HEADER_GRIP_LAST] = it }
        previousId?.let { result[GripConstants.HEADER_GRIP_PREVIOUS_ID] = it }
        
        result.putAll(headers)
        
        return result
    }
    
    /**
     * Applies the GRIP headers to a ResponseEntity.Builder.
     */
    fun <T> applyTo(builder: ResponseEntity.BodyBuilder): ResponseEntity.BodyBuilder {
        build().forEach { (key, value) ->
            builder.header(key, value)
        }
        return builder
    }
}