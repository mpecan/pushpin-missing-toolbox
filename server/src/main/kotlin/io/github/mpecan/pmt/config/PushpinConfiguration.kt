package io.github.mpecan.pmt.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Pushpin.
 * Enables the PushpinProperties configuration properties.
 */
@Configuration
@EnableConfigurationProperties(PushpinProperties::class)
class PushpinConfiguration
