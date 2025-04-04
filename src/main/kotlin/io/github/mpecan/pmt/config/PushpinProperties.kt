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
 */
@ConfigurationProperties(prefix = "pushpin")
data class PushpinProperties(
    val servers: List<ServerProperties> = emptyList(),
    val defaultTimeout: Long = 5000,
    val healthCheckEnabled: Boolean = true,
    val healthCheckInterval: Long = 60000,
    val authEnabled: Boolean = false,
    val authSecret: String = ""
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
        val healthCheckPath: String = "/status"
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