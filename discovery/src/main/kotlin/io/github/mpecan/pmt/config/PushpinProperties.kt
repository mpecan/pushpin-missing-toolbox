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
 * @property zmqHwm High water mark for ZMQ sockets (max messages in queue)
 * @property zmqLinger Linger period for ZMQ sockets in milliseconds
 * @property zmqReconnectIvl Initial reconnection interval in milliseconds
 * @property zmqReconnectIvlMax Maximum reconnection interval in milliseconds
 * @property zmqSendTimeout Send timeout in milliseconds
 * @property zmqConnectionPoolEnabled Whether to use a persistent connection pool for ZMQ
 * @property zmqConnectionPoolRefreshInterval Interval for refreshing the connection pool in milliseconds
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
    // ZMQ settings - always using PUSH socket type for Pushpin compatibility
    val zmqHwm: Int = 1000,
    val zmqLinger: Int = 0,
    val zmqReconnectIvl: Int = 100,
    val zmqReconnectIvlMax: Int = 10000,
    val zmqSendTimeout: Int = 1000,
    val zmqConnectionPoolEnabled: Boolean = true,
    val zmqConnectionPoolRefreshInterval: Long = 60000,
    val testMode: Boolean = false
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
        val active: Boolean = true,
        val weight: Int = 100,
        val healthCheckPath: String = "/api/health/check"
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
            active = active,
            weight = weight,
            healthCheckPath = healthCheckPath
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
        val hmac: HmacProperties = HmacProperties()
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
        val authoritiesPrefix: String = "ROLE_"
    )

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
        val refillTimeInMillis: Long = 60000 // 1 minute
    )

    /**
     * Audit logging properties.
     *
     * @property enabled Whether audit logging is enabled
     * @property level Audit logging level
     */
    data class AuditLoggingProperties(
        val enabled: Boolean = false,
        val level: String = "INFO"
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
        val secretKey: String = ""
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
        val headerName: String = "X-Pushpin-Signature"
    )
}