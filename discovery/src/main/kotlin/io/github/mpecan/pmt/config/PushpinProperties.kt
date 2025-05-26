package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.model.PushpinServer
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Pushpin servers.
 *
 * @property servers List of Pushpin servers
 * @property defaultTimeout Default timeout for requests to Pushpin servers in milliseconds
 * @property healthCheckEnabled Whether to enable health checks for Pushpin servers
 * @property healthCheckInterval Interval for health checks in milliseconds
 * @property authEnabled Whether to enable authentication for Pushpin servers
 * @property authSecret Secret for authentication
 * @property security Security configuration properties
 * @property zmqEnabled Whether to use ZMQ for publishing messages instead of HTTP
 * @property testMode Whether test mode is enabled
 */
@ConfigurationProperties(prefix = "pushpin")
data class PushpinProperties(
    val servers: List<ServerProperties> = emptyList(),
    val defaultTimeout: Long = 5000,
    val healthCheckEnabled: Boolean = true,
    val healthCheckInterval: Long = 60000,
    val authEnabled: Boolean = false,
    val authSecret: String = "",
    val security: SecurityProperties = SecurityProperties(),
    val zmqEnabled: Boolean = false,
    val testMode: Boolean = false,
) {
    /**
     * Configuration properties for a Pushpin server.
     */
    data class ServerProperties(
        val id: String,
        val host: String,
        val port: Int,
        val controlPort: Int = 5564,
        val publishPort: Int = 5560,
        val httpPort: Int = 8080,
        val active: Boolean = true,
        val weight: Int = 100,
        val healthCheckPath: String = "/api/health/check",
    ) {
        /**
         * Converts to a PushpinServer model.
         */
        fun toPushpinServer(): PushpinServer = PushpinServer(
            id = id,
            host = host,
            port = port,
            controlPort = controlPort,
            publishPort = publishPort,
            httpPort = httpPort,
            active = active,
            weight = weight,
            healthCheckPath = healthCheckPath,
        )
    }

    /**
     * Security configuration properties.
     *
     * @property jwt JWT authentication properties
     * @property rateLimit Rate limiting properties
     * @property auditLogging Audit logging properties
     * @property encryption Encryption properties for sensitive channel data
     */
    data class SecurityProperties(
        val jwt: JwtProperties = JwtProperties(),
        val rateLimit: RateLimitProperties = RateLimitProperties(),
        val auditLogging: AuditLoggingProperties = AuditLoggingProperties(),
        val encryption: EncryptionProperties = EncryptionProperties(),
        val hmac: HmacProperties = HmacProperties(),
    )

    /**
     * JWT authentication properties.
     *
     * @property enabled Whether JWT authentication is enabled
     * @property provider The JWT provider to use (symmetric, keycloak, auth0, etc.)
     * @property secret Secret key for JWT token signing (used only when provider is 'symmetric')
     * @property jwksUri URI for JSON Web Key Set for OAuth2 providers like Keycloak
     * @property issuer Issuer identifier for the JWT tokens
     * @property audience Audience identifier for the JWT tokens
     * @property expirationMs Token expiration time in milliseconds (for token generation)
     * @property clientId Client ID for OAuth2 providers (when applicable)
     * @property clientSecret Client secret for OAuth2 providers (when applicable)
     * @property authoritiesClaim The name of the claim containing the user roles/authorities
     * @property authoritiesPrefix Prefix to add to authorities extracted from the JWT
     * @property claimExtraction Configuration for extracting specific claims from the JWT token
     * @property remoteAuthorization Configuration for remote authorization API
     */
    data class JwtProperties(
        val enabled: Boolean = false,
        val provider: String = "symmetric", // "symmetric", "keycloak", "auth0", etc.
        val secret: String = "changemechangemechangemechangemechangemechangeme",
        val jwksUri: String = "",
        val issuer: String = "pushpin-missing-toolbox",
        val audience: String = "pushpin-client",
        val expirationMs: Long = 3600000, // 1 hour
        val clientId: String = "",
        val clientSecret: String = "",
        val authoritiesClaim: String = "roles",
        val authoritiesPrefix: String = "ROLE_",
        val claimExtraction: ClaimExtractionProperties = ClaimExtractionProperties(),
        val remoteAuthorization: RemoteAuthorizationProperties = RemoteAuthorizationProperties(),
    ) {
        /**
         * Configuration for extracting specific claims from JWT tokens and transforming
         * them for use in downstream services or authorization decisions.
         *
         * @property enabled Whether claim extraction is enabled
         * @property userIdClaim The claim to use as the user identifier
         * @property extractClaims List of specific claims to extract from the JWT
         * @property headerPrefix Prefix to add to header names when including claims in headers
         * @property includeAllClaims Whether to include all available claims from the JWT
         * @property transformations Custom transformations to apply to extracted claims
         */
        data class ClaimExtractionProperties(
            val enabled: Boolean = false,
            val userIdClaim: String = "sub",
            val extractClaims: List<String> = listOf("sub", "email"),
            val headerPrefix: String = "X-JWT-Claim-",
            val includeAllClaims: Boolean = false,
            val transformations: Map<String, String> = emptyMap(),
        )

        /**
         * Configuration for using a remote authorization API to validate access to resources.
         *
         * @property enabled Whether remote authorization is enabled
         * @property url The URL of the remote authorization API
         * @property method HTTP method to use for the authorization request (GET, POST)
         * @property timeout Timeout for the authorization request in milliseconds
         * @property cacheEnabled Whether to cache authorization results
         * @property cacheTtl Time-to-live for cached authorization results in milliseconds
         * @property cacheMaxSize Maximum number of entries in the authorization cache
         * @property includeHeaders List of request headers to include in the authorization request
         * @property includeQueryParams Whether to include query parameters in the authorization request
         * @property headerName Header name for conveying the authorization decision to downstream services
         */
        data class RemoteAuthorizationProperties(
            val enabled: Boolean = false,
            val url: String = "",
            val method: String = "POST",
            val timeout: Long = 5000,
            val cacheEnabled: Boolean = true,
            val cacheTtl: Long = 300000, // 5 minutes
            val cacheMaxSize: Long = 1000,
            val includeHeaders: List<String> = listOf("Authorization"),
            val includeQueryParams: Boolean = true,
            val headerName: String = "X-Auth-Decision",
        )
    }

    /**
     * Rate limiting properties.
     *
     * @property enabled Whether rate limiting is enabled
     * @property capacity Number of requests allowed in the time period
     * @property refillTimeInMillis Time period for refilling tokens in milliseconds
     */
    data class RateLimitProperties(
        val enabled: Boolean = false,
        val capacity: Long = 100,
        val refillTimeInMillis: Long = 60000, // 1 minute
    )

    /**
     * Audit logging properties.
     *
     * @property enabled Whether audit logging is enabled
     * @property level Audit logging level
     */
    data class AuditLoggingProperties(
        val enabled: Boolean = false,
        val level: String = "INFO",
    )

    /**
     * Encryption properties for sensitive channel data.
     *
     * @property enabled Whether encryption is enabled
     * @property algorithm Encryption algorithm
     * @property secretKey Secret key for encryption
     */
    data class EncryptionProperties(
        val enabled: Boolean = false,
        val algorithm: String = "AES/GCM/NoPadding",
        val secretKey: String = "",
    )

    /**
     * HMAC request signing properties for server-to-server communication.
     *
     * @property enabled Whether HMAC request signing is enabled
     * @property algorithm HMAC algorithm
     * @property secretKey Secret key for HMAC
     * @property headerName Name of the header for the HMAC signature
     */
    data class HmacProperties(
        val enabled: Boolean = false,
        val algorithm: String = "HmacSHA256",
        val secretKey: String = "",
        val headerName: String = "X-Pushpin-Signature",
    )
}
