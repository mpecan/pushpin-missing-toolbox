package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.model.PushpinServer
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

/**
 * A discovery mechanism that discovers Pushpin servers from Kubernetes pods.
 * 
 * This is a placeholder implementation that demonstrates how the system can be extended
 * with different discovery mechanisms. In a real implementation, this would use the Kubernetes API
 * to discover pods with specific labels and convert them to PushpinServer instances.
 */
class KubernetesDiscovery(
    private val properties: KubernetesDiscoveryProperties
) : PushpinDiscovery {

    private val logger = LoggerFactory.getLogger(KubernetesDiscovery::class.java)

    override val id: String = "kubernetes"

    override fun discoverServers(): Flux<PushpinServer> {
        logger.debug("Discovering Pushpin servers from Kubernetes pods")
        
        // This is a placeholder implementation
        // In a real implementation, this would use the Kubernetes API to discover pods
        // with specific labels and convert them to PushpinServer instances
        
        // For now, just return an empty flux
        return Flux.empty()
    }

    override fun isEnabled(): Boolean {
        return properties.enabled
    }
}