package io.github.mpecan.pmt.security.ratelimit.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.ratelimit.RateLimitFilter
import io.github.mpecan.pmt.security.ratelimit.RateLimitProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for rate limiting functionality.
 */
@AutoConfiguration
@ConditionalOnClass(RateLimitFilter::class)
@ConditionalOnProperty(
    prefix = "pushpin.security.rate-limit",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitAutoConfiguration {

    /**
     * Creates a RateLimitFilter bean if one doesn't already exist.
     */
    @Bean
    @ConditionalOnMissingBean
    fun rateLimitFilter(properties: RateLimitProperties, auditService: AuditService): RateLimitFilter {
        return RateLimitFilter(properties, auditService)
    }
}
