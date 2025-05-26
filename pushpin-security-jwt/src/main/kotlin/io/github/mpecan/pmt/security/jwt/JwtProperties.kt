package io.github.mpecan.pmt.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for JWT functionality.
 */
@ConfigurationProperties(prefix = "pushpin.security.jwt")
data class JwtProperties(
    /**
     * Whether JWT authentication is enabled.
     */
    val enabled: Boolean = false,
    
    /**
     * JWT provider type (keycloak, auth0, okta, oauth2, symmetric).
     */
    val provider: String = "symmetric",
    
    /**
     * Secret key for symmetric JWT signing (required when provider is 'symmetric').
     */
    val secret: String = "",
    
    /**
     * JWKS URI for asymmetric JWT verification (required for oauth2 providers).
     */
    val jwksUri: String = "",
    
    /**
     * Expected issuer of the JWT tokens.
     */
    val issuer: String = "",
    
    /**
     * Expected audience of the JWT tokens.
     */
    val audience: String = "",
    
    /**
     * Claim name containing user authorities/roles.
     */
    val authoritiesClaim: String = "scope",
    
    /**
     * Prefix for authorities extracted from JWT.
     */
    val authoritiesPrefix: String = "SCOPE_",
    
    /**
     * Claim extraction configuration.
     */
    val claimExtraction: ClaimExtractionProperties = ClaimExtractionProperties()
)

/**
 * Configuration for extracting claims from JWT tokens.
 */
data class ClaimExtractionProperties(
    /**
     * Whether claim extraction is enabled.
     */
    val enabled: Boolean = false,
    
    /**
     * List of claim paths to extract.
     */
    val extractClaims: List<String> = listOf("$.channels")
)