package io.github.mpecan.pmt.transport.health

import io.github.mpecan.pmt.model.PushpinServer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Interface for transport-specific health checking of Pushpin servers.
 * 
 * Each transport implementation (HTTP, ZMQ, etc.) should provide its own
 * health checking mechanism appropriate for that transport protocol.
 */
interface TransportHealthChecker {
    /**
     * Checks the health of a single server using transport-specific methods.
     * 
     * @param server The server to check
     * @return A Mono that emits true if the server is healthy, false otherwise
     */
    fun checkHealth(server: PushpinServer): Mono<Boolean>
    
    /**
     * Checks multiple servers in parallel.
     * 
     * @param servers The list of servers to check
     * @return A Mono containing a map of server IDs to their health status
     */
    fun checkAllServers(servers: List<PushpinServer>): Mono<Map<String, Boolean>> {
        if (servers.isEmpty()) {
            return Mono.just(emptyMap())
        }
        
        val healthChecks = servers.map { server ->
            checkHealth(server)
                .map { isHealthy -> server.id to isHealthy }
                .onErrorReturn(server.id to false)
        }
        
        return Flux.merge(healthChecks)
            .collectMap({ it.first }, { it.second })
    }
    
    /**
     * Gets the transport type this health checker supports.
     * 
     * @return The transport type (e.g., "http", "zmq")
     */
    fun getTransportType(): String
}