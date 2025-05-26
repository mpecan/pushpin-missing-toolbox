package io.github.mpecan.pmt.security.core

import org.springframework.security.oauth2.jwt.Jwt

/**
 * Service for extracting claims from JWT tokens.
 */
interface ClaimExtractorService {
    /**
     * Extract a string claim from the JWT token.
     *
     * @param jwt The JWT token
     * @param claimPath Path to the claim using the syntax defined by the implementation
     * @return The extracted claim value, or null if not found
     */
    fun extractStringClaim(jwt: Jwt, claimPath: String): String?

    /**
     * Extract a list claim from the JWT token.
     *
     * @param jwt The JWT token
     * @param claimPath Path to the claim using the syntax defined by the implementation
     * @return The extracted claim values as a list, or empty list if not found
     */
    fun extractListClaim(jwt: Jwt, claimPath: String): List<String>

    /**
     * Extract a map claim from the JWT token.
     *
     * @param jwt The JWT token
     * @param claimPath Path to the claim using the syntax defined by the implementation
     * @return The extracted claim values as a map, or empty map if not found
     */
    fun extractMapClaim(jwt: Jwt, claimPath: String): Map<String, Any>

    /**
     * Check if a claim exists in the JWT token.
     *
     * @param jwt The JWT token
     * @param claimPath Path to the claim using the syntax defined by the implementation
     * @return True if the claim exists, false otherwise
     */
    fun hasClaim(jwt: Jwt, claimPath: String): Boolean
}
