package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.model.PushpinServer
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Pushpin servers.
 *
 * @property servers List of Pushpin servers
 * @property defaultTimeout Default timeout for requests to Pushpin servers in milliseconds
 * @property healthCheckEnabled Whether to enable health checks for Pushpin servers
 * @property healthCheckInterval Interval for health checks in milliseconds
 * @property authEnabled Whether to enable authentication for Pushpin servers
 * @property authSecret Secret for authentication
 * @property zmqEnabled Whether to use ZMQ for publishing messages instead of HTTP
 * @property zmqHwm High water mark for ZMQ sockets (max messages in queue)
 * @property zmqLinger Linger period for ZMQ sockets in milliseconds
 * @property zmqReconnectIvl Initial reconnection interval in milliseconds
 * @property zmqReconnectIvlMax Maximum reconnection interval in milliseconds
 * @property zmqSendTimeout Send timeout in milliseconds
 * @property zmqConnectionPoolEnabled Whether to use a persistent connection pool for ZMQ
 * @property zmqConnectionPoolRefreshInterval Interval for refreshing the connection pool in milliseconds
 * @property testMode Whether test mode is enabled
 */
@ConfigurationProperties(prefix = "pushpin")
data class PushpinProperties(
    val servers: List<ServerProperties> = emptyList(),
    val defaultTimeout: Long = 5000,
    val healthCheckEnabled: Boolean = true,
    val healthCheckInterval: Long = 60000,
    val authEnabled: Boolean = false,
    val authSecret: String = "",
    val zmqEnabled: Boolean = false,
    // ZMQ settings - always using PUSH socket type for Pushpin compatibility
    val zmqHwm: Int = 1000,
    val zmqLinger: Int = 0,
    val zmqReconnectIvl: Int = 100,
    val zmqReconnectIvlMax: Int = 10000,
    val zmqSendTimeout: Int = 1000,
    val zmqConnectionPoolEnabled: Boolean = true,
    val zmqConnectionPoolRefreshInterval: Long = 60000,
    val testMode: Boolean = false
) {
    /**
     * Configuration properties for a Pushpin server.
     */
    data class ServerProperties(
        val id: String,
        val host: String,
        val port: Int,
        val controlPort: Int = 5564,
        val publishPort: Int = 5560,
        val active: Boolean = true,
        val weight: Int = 100,
        val healthCheckPath: String = "/api/health/check"
    ) {
        /**
         * Converts to a PushpinServer model.
         */
        fun toPushpinServer(): PushpinServer = PushpinServer(
            id = id,
            host = host,
            port = port,
            controlPort = controlPort,
            publishPort = publishPort,
            active = active,
            weight = weight,
            healthCheckPath = healthCheckPath
        )
    }

}