package io.github.mpecan.pmt.security.hmac.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.HmacService
import io.github.mpecan.pmt.security.core.NoOpHmacService
import io.github.mpecan.pmt.security.hmac.DefaultHmacService
import io.github.mpecan.pmt.security.hmac.HmacProperties
import io.github.mpecan.pmt.security.hmac.HmacSignatureFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Boot AutoConfiguration for HMAC functionality.
 */
@Configuration
@EnableConfigurationProperties(HmacProperties::class)
class HmacAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.hmac",
        name = ["enabled"],
        havingValue = "true",
    )
    fun hmacService(properties: HmacProperties): HmacService {
        return DefaultHmacService(properties)
    }

    @Bean
    @ConditionalOnMissingBean(HmacService::class)
    fun noOpHmacService(): HmacService {
        return NoOpHmacService()
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.hmac",
        name = ["enabled"],
        havingValue = "true",
    )
    fun hmacSignatureFilter(
        hmacService: HmacService,
        properties: HmacProperties,
        auditService: AuditService,
    ): HmacSignatureFilter {
        return HmacSignatureFilter(hmacService, properties, auditService)
    }
}
