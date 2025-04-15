package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.model.PushpinServer
import reactor.core.publisher.Flux

/**
 * Interface for discovering Pushpin servers.
 * 
 * Implementations of this interface provide different mechanisms for discovering Pushpin servers,
 * such as configuration-based, AWS-based, Kubernetes-based, etc.
 */
interface PushpinDiscovery {
    /**
     * The unique identifier for this discovery mechanism.
     */
    val id: String
    
    /**
     * Discovers Pushpin servers.
     * 
     * @return A Flux of PushpinServer instances.
     */
    fun discoverServers(): Flux<PushpinServer>
    
    /**
     * Checks if this discovery mechanism is enabled.
     * 
     * @return true if enabled, false otherwise.
     */
    fun isEnabled(): Boolean
}