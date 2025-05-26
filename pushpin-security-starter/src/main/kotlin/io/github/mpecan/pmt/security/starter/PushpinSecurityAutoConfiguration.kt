package io.github.mpecan.pmt.security.starter

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Import

/**
 * Auto-configuration for Pushpin Security Starter.
 * * This starter automatically includes all Pushpin security modules:
 * - pushpin-security-core: Core interfaces and models
 * - pushpin-security-remote: Remote authorization client
 * - pushpin-security-audit: Audit logging service
 * - pushpin-security-encryption: Encryption service
 * - pushpin-security-hmac: HMAC signing service
 * - pushpin-security-jwt: JWT processing service
 * * Each module has its own auto-configuration that can be enabled/disabled
 * via properties.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = ["org.springframework.security.config.annotation.web.configuration.EnableWebSecurity"])
@Import(
    io.github.mpecan.pmt.security.core.config.SecurityCoreAutoConfiguration::class,
    io.github.mpecan.pmt.security.remote.config.RemoteAuthorizationAutoConfiguration::class,
    io.github.mpecan.pmt.security.remote.config.NoOpRemoteAuthorizationAutoConfiguration::class,
    io.github.mpecan.pmt.security.audit.config.AuditAutoConfiguration::class,
    io.github.mpecan.pmt.security.encryption.config.EncryptionAutoConfiguration::class,
    io.github.mpecan.pmt.security.hmac.config.HmacAutoConfiguration::class,
    io.github.mpecan.pmt.security.jwt.config.JwtAutoConfiguration::class,
)
class PushpinSecurityAutoConfiguration
