package io.github.mpecan.pmt.security.hmac

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for HMAC signing.
 */
@ConfigurationProperties(prefix = "pushpin.security.hmac")
data class HmacProperties(
    /**
     * Whether HMAC signing is enabled.
     */
    val enabled: Boolean = false,
    
    /**
     * The secret key for HMAC signing.
     */
    val secretKey: String = "",
    
    /**
     * The HMAC algorithm to use (e.g., HmacSHA256).
     */
    val algorithm: String = "HmacSHA256",
    
    /**
     * The header name for the HMAC signature.
     */
    val headerName: String = "X-Pushpin-Signature",
    
    /**
     * The maximum age of a request in milliseconds.
     */
    val maxAgeMs: Long = 300000, // 5 minutes
    
    /**
     * Request paths that are excluded from HMAC verification.
     */
    val excludedPaths: List<String> = listOf(
        "/api/public/",
        "/actuator/",
        "/api/pushpin/auth"
    )
)