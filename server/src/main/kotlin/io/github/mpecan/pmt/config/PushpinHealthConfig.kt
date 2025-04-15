package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.health.DefaultPushpinHealthChecker
import io.github.mpecan.pmt.health.PushpinHealthChecker
import io.github.mpecan.pmt.service.PushpinService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for Pushpin health checking.
 */
@Configuration
class PushpinHealthConfig {

    /**
     * Creates a WebClient bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }

    /**
     * Creates a PushpinHealthChecker bean if none is provided.
     */
    @Bean
    @ConditionalOnMissingBean
    fun pushpinHealthChecker(
        webClient: WebClient,
        pushpinProperties: PushpinProperties,
        pushpinService: PushpinService
    ): PushpinHealthChecker {
        return DefaultPushpinHealthChecker(
            webClient,
            pushpinProperties.healthCheckEnabled,
            pushpinProperties.defaultTimeout,
            pushpinService
        )
    }
}
