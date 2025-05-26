package io.github.mpecan.pmt.security.audit.config

import io.github.mpecan.pmt.security.audit.AuditProperties
import io.github.mpecan.pmt.security.audit.DefaultAuditLogService
import io.github.mpecan.pmt.security.core.AuditService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Spring Boot auto-configuration for audit logging.
 * When audit is enabled, this configuration will replace the default NoOpAuditService
 * from pushpin-security-core with a real implementation.
 */
@AutoConfiguration
@EnableConfigurationProperties(AuditProperties::class)
@ConditionalOnProperty(
    prefix = "pushpin.security.audit",
    name = ["enabled"],
    havingValue = "true"
)
class AuditAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(AuditService::class)
    fun auditService(properties: AuditProperties): AuditService {
        return DefaultAuditLogService(properties)
    }
}