package io.github.mpecan.pmt.security.starter

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Unified configuration properties for all Pushpin security modules.
 * This class aggregates all security-related configurations in one place.
 */
@ConfigurationProperties(prefix = "pushpin.security")
data class PushpinSecurityProperties(
    /**
     * Whether security features are globally enabled.
     */
    val enabled: Boolean = true,
    
    /**
     * Remote authorization configuration.
     */
    val remote: RemoteProperties = RemoteProperties(),
    
    /**
     * Audit logging configuration.
     */
    val audit: AuditProperties = AuditProperties(),
    
    /**
     * Encryption configuration.
     */
    val encryption: EncryptionProperties = EncryptionProperties(),
    
    /**
     * HMAC signing configuration.
     */
    val hmac: HmacProperties = HmacProperties(),
    
    /**
     * JWT configuration.
     */
    val jwt: JwtProperties = JwtProperties()
)

/**
 * Remote authorization properties.
 */
data class RemoteProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val timeout: Long = 30000,
    val retryAttempts: Int = 3,
    val retryDelayMs: Long = 1000,
    val cache: CacheProperties = CacheProperties()
)

/**
 * Cache configuration properties.
 */
data class CacheProperties(
    val enabled: Boolean = true,
    val maxSize: Long = 1000,
    val expireAfterWriteMinutes: Long = 5,
    val expireAfterAccessMinutes: Long = 10
)

/**
 * Audit logging properties.
 */
data class AuditProperties(
    val enabled: Boolean = true,
    val includeSensitiveData: Boolean = false,
    val maxEventLength: Int = 1000
)

/**
 * Encryption properties.
 */
data class EncryptionProperties(
    val enabled: Boolean = false,
    val algorithm: String = "AES/GCM/NoPadding",
    val keySize: Int = 256,
    val secretKey: String = ""
)

/**
 * HMAC properties.
 */
data class HmacProperties(
    val enabled: Boolean = false,
    val secretKey: String = "",
    val algorithm: String = "HmacSHA256",
    val headerName: String = "X-Pushpin-Signature",
    val maxAgeMs: Long = 300000,
    val excludedPaths: List<String> = listOf("/api/public/", "/actuator/", "/api/pushpin/auth")
)

/**
 * JWT properties.
 */
data class JwtProperties(
    val enabled: Boolean = false,
    val provider: String = "symmetric",
    val secret: String = "",
    val jwksUri: String = "",
    val issuer: String = "",
    val audience: String = "",
    val authoritiesClaim: String = "scope",
    val authoritiesPrefix: String = "SCOPE_",
    val claimExtraction: ClaimExtractionProperties = ClaimExtractionProperties()
)

/**
 * JWT claim extraction properties.
 */
data class ClaimExtractionProperties(
    val enabled: Boolean = false,
    val extractClaims: List<String> = listOf("$.channels")
)