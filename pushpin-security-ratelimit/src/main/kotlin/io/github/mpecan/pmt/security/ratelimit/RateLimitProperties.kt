package io.github.mpecan.pmt.security.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for rate limiting.
 *
 * @property enabled Whether rate limiting is enabled
 * @property capacity Number of requests allowed in the time period
 * @property refillTimeInMillis Time period for refilling tokens in milliseconds
 */
@ConfigurationProperties(prefix = "pushpin.security.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = false,
    val capacity: Long = 100,
    val refillTimeInMillis: Long = 60000, // 1 minute
)
