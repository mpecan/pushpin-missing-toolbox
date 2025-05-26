package io.github.mpecan.pmt.transport.zmq

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for ZMQ transport.
 */
@ConfigurationProperties(prefix = "pushpin.transport.zmq")
data class ZmqTransportProperties(
    /**
     * Whether ZMQ connection pooling is enabled.
     */
    val connectionPoolEnabled: Boolean = true,

    /**
     * High water mark for ZMQ sockets.
     */
    val hwm: Int = 1000,

    /**
     * Linger time for ZMQ sockets in milliseconds.
     */
    val linger: Int = 1000,

    /**
     * Send timeout for ZMQ sockets in milliseconds.
     */
    val sendTimeout: Int = 1000,

    /**
     * Reconnection interval for ZMQ sockets in milliseconds.
     */
    val reconnectIvl: Int = 100,

    /**
     * Maximum reconnection interval for ZMQ sockets in milliseconds.
     */
    val reconnectIvlMax: Int = 0,

    /**
     * Connection pool refresh interval in milliseconds.
     */
    val connectionPoolRefreshInterval: Long = 60000L,
)
