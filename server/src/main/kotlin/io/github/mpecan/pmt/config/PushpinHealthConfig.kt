package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.health.DefaultPushpinHealthChecker
import io.github.mpecan.pmt.health.PushpinHealthChecker
import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.transport.health.TransportHealthChecker
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Pushpin health checking.
 */
@Configuration
class PushpinHealthConfig {
    /**
     * Creates a PushpinHealthChecker bean if none is provided.
     * * This bean now supports multiple transport health checkers and will
     * select the appropriate one based on the configured transport type.
     */
    @Bean
    @ConditionalOnMissingBean
    fun pushpinHealthChecker(
        transportHealthCheckers: List<TransportHealthChecker>,
        pushpinProperties: PushpinProperties,
        pushpinService: PushpinService,
        metricsService: MetricsService,
        @Value("\${pushpin.transport:http}") transportType: String,
    ): PushpinHealthChecker =
        DefaultPushpinHealthChecker(
            transportHealthCheckers,
            pushpinProperties.healthCheckEnabled,
            pushpinService,
            metricsService,
            transportType,
        )
}
