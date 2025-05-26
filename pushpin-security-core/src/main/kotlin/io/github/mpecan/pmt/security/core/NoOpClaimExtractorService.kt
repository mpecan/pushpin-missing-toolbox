package io.github.mpecan.pmt.security.core

import org.springframework.security.oauth2.jwt.Jwt

/**
 * No-op implementation of ClaimExtractorService that is used when JWT functionality is not available.
 */
class NoOpClaimExtractorService : ClaimExtractorService {
    override fun extractStringClaim(jwt: Jwt, claimPath: String): String? = null

    override fun extractListClaim(jwt: Jwt, claimPath: String): List<String> = emptyList()

    override fun extractMapClaim(jwt: Jwt, claimPath: String): Map<String, Any> = emptyMap()

    override fun hasClaim(jwt: Jwt, claimPath: String): Boolean = false
}
