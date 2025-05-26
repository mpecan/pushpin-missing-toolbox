package io.github.mpecan.pmt.grip.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * Helper for GRIP authentication using JWT.
 */
@Suppress("unused")
object GripAuthHelper {

    /**
     * Creates a GRIP signature JWT token.
     *
     * @param iss The issuer claim
     * @param key The secret key as a string
     * @param expiresIn Expiration time in seconds from now (optional)
     * @return The JWT token string
     */
    fun createGripSignature(iss: String, key: String, expiresIn: Long? = null): String {
        val secretKey = Keys.hmacShaKeyFor(key.toByteArray(StandardCharsets.UTF_8))
        return createGripSignature(iss, secretKey, expiresIn)
    }

    /**
     * Creates a GRIP signature JWT token.
     *
     * @param iss The issuer claim
     * @param key The secret key
     * @param expiresIn Expiration time in seconds from now (optional)
     * @return The JWT token string
     */
    fun createGripSignature(iss: String, key: SecretKey, expiresIn: Long? = null): String {
        val builder = Jwts.builder()
            .issuer(iss)
            .issuedAt(Date.from(Instant.now()))

        if (expiresIn != null) {
            builder.expiration(Date.from(Instant.now().plusSeconds(expiresIn)))
        }

        return builder
            .signWith(key)
            .compact()
    }

    /**
     * Validates a GRIP signature JWT token.
     *
     * @param token The JWT token to validate
     * @param key The secret key as a string
     * @return True if the token is valid, false otherwise
     */
    fun validateGripSignature(token: String, key: String): Boolean {
        return try {
            val secretKey = Keys.hmacShaKeyFor(key.toByteArray(StandardCharsets.UTF_8))
            validateGripSignature(token, secretKey)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates a GRIP signature JWT token.
     *
     * @param token The JWT token to validate
     * @param key The secret key
     * @return True if the token is valid, false otherwise
     */
    fun validateGripSignature(token: String, key: SecretKey): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts the issuer from a GRIP signature JWT token.
     *
     * @param token The JWT token
     * @param key The secret key as a string
     * @return The issuer claim, or null if invalid
     */
    fun extractIssuer(token: String, key: String): String? {
        return try {
            val secretKey = Keys.hmacShaKeyFor(key.toByteArray(StandardCharsets.UTF_8))
            extractIssuer(token, secretKey)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts the issuer from a GRIP signature JWT token.
     *
     * @param token The JWT token
     * @param key The secret key
     * @return The issuer claim, or null if invalid
     */
    fun extractIssuer(token: String, key: SecretKey): String? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.issuer
        } catch (e: Exception) {
            null
        }
    }
}
