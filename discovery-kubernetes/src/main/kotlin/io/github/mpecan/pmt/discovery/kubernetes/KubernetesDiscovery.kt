package io.github.mpecan.pmt.discovery.kubernetes

import io.github.mpecan.pmt.discovery.PushpinDiscovery
import io.github.mpecan.pmt.discovery.kubernetes.clients.KubernetesClientProvider
import io.github.mpecan.pmt.discovery.kubernetes.converter.PodConverter
import io.github.mpecan.pmt.discovery.kubernetes.health.PodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.pods.KubernetesPodProvider
import io.github.mpecan.pmt.model.PushpinServer
import io.kubernetes.client.openapi.models.V1Pod
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A discovery mechanism that discovers Pushpin servers from Kubernetes pods.
 * 
 * This implementation uses the Kubernetes API to discover pods with specific labels
 * and converts them to PushpinServer instances. It supports filtering by labels
 * and can be configured to use either pod IPs or node ports.
 */
open class KubernetesDiscovery(
    private val properties: KubernetesDiscoveryProperties,
    private val podProvider: KubernetesPodProvider,
    private val podHealthChecker: PodHealthChecker,
    private val podConverter: PodConverter
) : PushpinDiscovery {

    private val logger = LoggerFactory.getLogger(KubernetesDiscovery::class.java)

    override val id: String = "kubernetes"
    
    // Cache of Kubernetes pods and last refresh time
    private val podCache = ConcurrentHashMap<String, V1Pod>()
    private var lastCacheRefresh: Instant = Instant.EPOCH
    
    /**
     * Discovers Pushpin servers from Kubernetes pods based on labels.
     */
    override fun discoverServers(): Flux<PushpinServer> {
        if (!isEnabled()) {
            logger.debug("Kubernetes discovery is disabled")
            return Flux.empty()
        }
        
        logger.debug("Discovering Pushpin servers from Kubernetes pods")
        
        return Flux.defer {
            refreshPodCacheIfNeeded()
            
            Flux.fromIterable(podCache.values)
                .filter { pod -> podHealthChecker.isHealthy(pod, properties) }
                .map { pod -> podConverter.toPushpinServer(pod, properties) }
                .doOnNext { server -> 
                    logger.debug("Discovered Pushpin server from Kubernetes: ${server.id} at ${server.getBaseUrl()}")
                }
                .doOnError { error ->
                    logger.error("Error discovering Pushpin servers from Kubernetes: ${error.message}", error)
                }
        }
    }

    /**
     * Refreshes the pod cache if needed based on the configured refresh interval.
     */
    private fun refreshPodCacheIfNeeded() {
        val now = Instant.now()
        val cacheExpiration = lastCacheRefresh.plus(Duration.ofSeconds(properties.refreshCacheSeconds.toLong()))
        
        if (now.isAfter(cacheExpiration)) {
            logger.debug("Refreshing Kubernetes pod cache")
            try {
                podCache.clear()
                
                // Discover pods
                val pods = podProvider.getPods(properties)
                pods.forEach { pod ->
                    val podName = pod.metadata?.name
                    if (podName != null) {
                        podCache[podName] = pod
                    }
                }
                
                lastCacheRefresh = now
                logger.debug("Kubernetes pod cache refreshed - found ${podCache.size} pods")
            } catch (e: Exception) {
                logger.error("Failed to refresh Kubernetes pod cache: ${e.message}", e)
                // Don't update lastCacheRefresh so we'll try again on the next call
            }
        }
    }

    override fun isEnabled(): Boolean {
        return properties.enabled
    }
}