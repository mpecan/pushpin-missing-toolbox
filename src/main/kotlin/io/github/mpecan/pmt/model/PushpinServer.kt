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
    val healthCheckPath: String = "/status"
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
     */
    fun getPublishUrl(): String = "tcp://$host:$publishPort"

    /**
     * Returns the health check URL for the server.
     */
    fun getHealthCheckUrl(): String = "${getBaseUrl()}$healthCheckPath"

    /**
     * Returns a URI for the server.
     */
    fun toUri(): URI = URI.create(getBaseUrl())
}