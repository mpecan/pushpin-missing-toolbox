package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.config.PushpinProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Configuration for the Pushpin discovery system.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(DiscoveryProperties::class)
class DiscoveryConfig {
    /**
     * Creates a ConfigurationBasedDiscovery bean.
     */
    @Bean
    fun configurationBasedDiscovery(
        discoveryProperties: DiscoveryProperties,
        pushpinProperties: PushpinProperties,
    ): ConfigurationBasedDiscovery =
        ConfigurationBasedDiscovery(
            discoveryProperties.configuration,
            pushpinProperties,
        )

    // AwsDiscovery bean is now provided by discovery-aws module via auto-configuration

    /**
     * Creates a PushpinDiscoveryManager bean.
     */
    @Bean
    fun pushpinDiscoveryManager(
        discoveryProperties: DiscoveryProperties,
        discoveries: List<PushpinDiscovery>,
    ): PushpinDiscoveryManager =
        PushpinDiscoveryManager(
            discoveryProperties,
            discoveries,
        )

    /**
     * Creates a bean for the discovery properties to be used in the @Scheduled annotation.
     */
    @Bean
    fun discoveryProperties(discoveryProperties: DiscoveryProperties): DiscoveryProperties = discoveryProperties
}
