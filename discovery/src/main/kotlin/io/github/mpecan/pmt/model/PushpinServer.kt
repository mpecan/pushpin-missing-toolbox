package io.github.mpecan.pmt.model

import java.net.URI

/**
 * Represents a Pushpin server instance.
 *
 * @property id Unique identifier for the server
 * @property host Hostname or IP address of the server
 * @property port HTTP port of the server
 * @property controlPort HTTP control port of the server
 * @property publishPort ZMQ publish port of the server
 * @property active Whether the server is active and should be used
 * @property weight Weight for load balancing (higher weight means more traffic)
 * @property healthCheckPath Path to use for health checks
 */
data class PushpinServer(
    val id: String,
    val host: String,
    val port: Int,
    val controlPort: Int = 5564,
    val publishPort: Int = 5560,
    val active: Boolean = true,
    val weight: Int = 100,
    val healthCheckPath: String = "/api/health/check",
) {
    /**
     * Returns the base URL for the server.
     */
    fun getBaseUrl(): String = "http://$host:$port"

    /**
     * Returns the control URL for the server.
     */
    fun getControlUrl(): String = "http://$host:$controlPort"

    /**
     * Returns the publish URL for the server.
     *
     * For ZeroMQ connections, this specifies the TCP endpoint for publishing messages.
     * Pushpin expects messages to be published to its PULL socket on port 5560 (by default).
     * We should connect (not bind) to this socket.
     */
    fun getPublishUrl(): String {
        // Format: tcp://host:port (ZeroMQ format)
        // This should point to Pushpin's PULL socket for publishing

        // Special case for localhost in testing to ensure container connectivity
        val effectiveHost = if (host == "localhost") "127.0.0.1" else host
        val url = "tcp://$effectiveHost:$publishPort"

        // For debugging purposes in logs
        println("ZMQ Publish URL for server $id: $url (connecting to Pushpin's PULL socket)")

        return url
    }

    /**
     * Returns the health check URL for the server.
     */
    fun getHealthCheckUrl(): String = "${getBaseUrl()}$healthCheckPath"

    /**
     * Returns a URI for the server.
     */
    fun toUri(): URI = URI.create(getBaseUrl())
}
