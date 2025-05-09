package io.github.mpecan.pmt.health

import io.github.mpecan.pmt.model.PushpinServer
import reactor.core.publisher.Mono

/**
 * Interface for checking the health of Pushpin servers.
 */
interface PushpinHealthChecker {
    /**
     * Checks the health of a single server.
     * * @param server The server to check
     * @return A Mono that emits true if the server is healthy, false otherwise
     */
    fun checkHealth(server: PushpinServer): Mono<Boolean>

    /**
     * Performs health checks on all servers.
     * * @return A map of server IDs to their health status (true if healthy, false otherwise)
     */
    fun checkServerHealth(): Map<String, PushpinServer>

    /**
     * Gets all healthy servers.
     * * @return A map of server IDs to healthy servers
     */
    fun getHealthyServers(): Map<String, PushpinServer>
}
