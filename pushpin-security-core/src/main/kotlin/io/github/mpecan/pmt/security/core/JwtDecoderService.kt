package io.github.mpecan.pmt.security.core

import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * Service for providing JWT decoders based on configuration.
 */
interface JwtDecoderService {
    /**
     * Get the appropriate JWT decoder based on configuration.
     */
    fun getDecoder(): JwtDecoder

    /**
     * Check if JWT decoding is enabled.
     */
    fun isJwtEnabled(): Boolean

    /**
     * Get the configured JWT provider.
     */
    fun getProvider(): String

    /**
     * Get the configured issuer.
     */
    fun getIssuer(): String

    /**
     * Get the configured audience.
     */
    fun getAudience(): String
}
