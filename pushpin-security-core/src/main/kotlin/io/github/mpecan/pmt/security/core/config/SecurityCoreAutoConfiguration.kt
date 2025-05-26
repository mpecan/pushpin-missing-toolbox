package io.github.mpecan.pmt.security.core.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.NoOpAuditService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for core security components.
 * Provides default implementations when no other beans are available.
 */
@AutoConfiguration
class SecurityCoreAutoConfiguration {
    
    /**
     * Provides a no-op AuditService when no other implementation is available.
     * This ensures that code using AuditService will not fail if no audit module is included.
     */
    @Bean
    @ConditionalOnMissingBean(AuditService::class)
    fun defaultAuditService(): AuditService {
        return NoOpAuditService()
    }
}