package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.security.ratelimit.RateLimitProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Bridge configuration to adapt PushpinProperties.RateLimitProperties
 * to the standalone RateLimitProperties for the rate limit module.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "pushpin.security.rate-limit",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class RateLimitBridgeConfig {
    /**
     * Creates a RateLimitProperties bean from PushpinProperties.
     * This allows the existing configuration to work with the new module.
     */
    @Bean
    fun rateLimitProperties(pushpinProperties: PushpinProperties): RateLimitProperties =
        RateLimitProperties(
            enabled = pushpinProperties.security.rateLimit.enabled,
            capacity = pushpinProperties.security.rateLimit.capacity,
            refillTimeInMillis = pushpinProperties.security.rateLimit.refillTimeInMillis,
        )
}
