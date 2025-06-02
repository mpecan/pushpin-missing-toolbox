package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.PushpinServer
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

/**
 * A discovery mechanism that uses the configuration properties to discover Pushpin servers.
 * This maintains backward compatibility with the existing configuration-based approach.
 */
class ConfigurationBasedDiscovery(
    private val properties: ConfigurationDiscoveryProperties,
    private val pushpinProperties: PushpinProperties,
) : PushpinDiscovery {
    private val logger = LoggerFactory.getLogger(ConfigurationBasedDiscovery::class.java)

    override val id: String = "configuration"

    override fun discoverServers(): Flux<PushpinServer> {
        logger.debug("Discovering Pushpin servers from configuration")

        return Flux
            .fromIterable(pushpinProperties.servers)
            .filter { it.active }
            .map { it.toPushpinServer() }
            .doOnNext { logger.debug("Discovered Pushpin server from configuration: ${it.id} at ${it.getBaseUrl()}") }
    }

    override fun isEnabled(): Boolean = properties.enabled
}
