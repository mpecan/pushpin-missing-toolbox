package io.github.mpecan.pmt.discovery

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for the Pushpin discovery system.
 *
 * @property enabled Whether the discovery system is enabled
 * @property refreshInterval The interval at which to refresh the server list
 * @property configuration Configuration for the configuration-based discovery
 */
@ConfigurationProperties(prefix = "pushpin.discovery")
data class DiscoveryProperties(
    val enabled: Boolean = true,
    val refreshInterval: Duration = Duration.ofMinutes(1),
    val configuration: ConfigurationDiscoveryProperties = ConfigurationDiscoveryProperties()
)

/**
 * Configuration properties for configuration-based discovery.
 *
 * @property enabled Whether configuration-based discovery is enabled
 */
data class ConfigurationDiscoveryProperties(
    val enabled: Boolean = true
)
