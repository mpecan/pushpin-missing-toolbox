package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.model.PushpinServer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the discovery of Pushpin servers using multiple discovery mechanisms.
 */
class PushpinDiscoveryManager(
    private val properties: DiscoveryProperties,
    private val discoveries: List<PushpinDiscovery>
) : InitializingBean {
    private val logger = LoggerFactory.getLogger(PushpinDiscoveryManager::class.java)
    private val servers = ConcurrentHashMap<String, PushpinServer>()

    /**
     * Initializes the discovery manager.
     */
    override fun afterPropertiesSet() {
        if (properties.enabled) {
            logger.info("Initializing Pushpin discovery manager with refresh interval: ${properties.refreshInterval}")
            refreshServers()
        } else {
            logger.info("Pushpin discovery manager is disabled")
        }
    }

    /**
     * Refreshes the list of servers from all enabled discovery mechanisms.
     * This method is called periodically based on the configured refresh interval.
     */
    @Scheduled(fixedDelayString = "#{@discoveryProperties.refreshInterval.toMillis()}")
    fun refreshServers() {
        if (!properties.enabled) {
            return
        }

        logger.debug("Refreshing Pushpin servers from all enabled discovery mechanisms")

        val enabledDiscoveries = discoveries.filter { it.isEnabled() }
        if (enabledDiscoveries.isEmpty()) {
            logger.warn("No enabled discovery mechanisms found")
            return
        }

        Flux.fromIterable(enabledDiscoveries)
            .flatMap { discovery ->
                discovery.discoverServers()
                    .doOnNext { server ->
                        logger.debug("Discovered server from ${discovery.id}: ${server.id} at ${server.getBaseUrl()}")
                    }
                    .onErrorResume { e ->
                        logger.error("Error discovering servers from ${discovery.id}: ${e.message}", e)
                        Flux.empty()
                    }
            }
            .collectList()
            .subscribe { discoveredServers ->
                // Update the servers map
                servers.clear()
                discoveredServers.forEach { server ->
                    servers[server.id] = server
                }
                logger.info("Updated Pushpin servers: ${servers.size} servers available")
            }
    }

    /**
     * Gets all discovered servers.
     */
    fun getAllServers(): List<PushpinServer> {
        return servers.values.toList()
    }

    /**
     * Gets a server by ID.
     */
    fun getServerById(id: String): PushpinServer? {
        return servers[id]
    }
}