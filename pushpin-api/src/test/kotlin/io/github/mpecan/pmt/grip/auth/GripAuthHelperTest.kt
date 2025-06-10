package io.github.mpecan.pmt.grip.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GripAuthHelperTest {
    private val testIssuer = "test-issuer"
    private val testKey = "this-is-a-test-key-that-is-long-enough-for-hmac"

    @Test
    fun `should create valid GRIP signature with string key`() {
        val signature = GripAuthHelper.createGripSignature(testIssuer, testKey)

        // Verify it's not empty
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        // Verify it's a valid JWT token
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(signature)
                .payload

        assertEquals(testIssuer, claims.issuer)
        assertNotNull(claims.issuedAt)
        assertNull(claims.expiration)
    }

    @Test
    fun `should create valid GRIP signature with SecretKey`() {
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val signature = GripAuthHelper.createGripSignature(testIssuer, secretKey)

        // Verify it's not empty
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        // Verify it's a valid JWT token
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(signature)
                .payload

        assertEquals(testIssuer, claims.issuer)
        assertNotNull(claims.issuedAt)
        assertNull(claims.expiration)
    }

    @Test
    fun `should create GRIP signature with expiration`() {
        val expiresIn = 3600L // 1 hour
        val signature = GripAuthHelper.createGripSignature(testIssuer, testKey, expiresIn)

        // Verify it's a valid JWT token with expiration
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(signature)
                .payload

        assertEquals(testIssuer, claims.issuer)
        assertNotNull(claims.issuedAt)
        assertNotNull(claims.expiration)

        // Verify expiration is approximately correct (within 5 seconds tolerance)
        val expectedExpiration = Instant.now().plusSeconds(expiresIn)
        val actualExpiration = claims.expiration.toInstant()
        val diffSeconds = Math.abs(actualExpiration.epochSecond - expectedExpiration.epochSecond)
        assertTrue(diffSeconds < 5, "Expiration time should be within 5 seconds of expected value")
    }

    @Test
    fun `should validate GRIP signature with string key`() {
        val signature = GripAuthHelper.createGripSignature(testIssuer, testKey)

        // Validate with correct key
        assertTrue(GripAuthHelper.validateGripSignature(signature, testKey))

        // Validate with incorrect key
        assertFalse(GripAuthHelper.validateGripSignature(signature, "wrong-key-that-is-long-enough-for-hmac"))

        // Validate invalid token
        assertFalse(GripAuthHelper.validateGripSignature("invalid-token", testKey))
    }

    @Test
    fun `should validate GRIP signature with SecretKey`() {
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val wrongKey = Keys.hmacShaKeyFor("wrong-key-that-is-long-enough-for-hmac".toByteArray(StandardCharsets.UTF_8))
        val signature = GripAuthHelper.createGripSignature(testIssuer, secretKey)

        // Validate with correct key
        assertTrue(GripAuthHelper.validateGripSignature(signature, secretKey))

        // Validate with incorrect key
        assertFalse(GripAuthHelper.validateGripSignature(signature, wrongKey))

        // Validate invalid token
        assertFalse(GripAuthHelper.validateGripSignature("invalid-token", secretKey))
    }

    @Test
    fun `should validate expired GRIP signature as invalid`() {
        // Create a token that's already expired
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val expiredToken =
            Jwts
                .builder()
                .issuer(testIssuer)
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(secretKey)
                .compact()

        assertFalse(GripAuthHelper.validateGripSignature(expiredToken, testKey))
    }

    @Test
    fun `should extract issuer from valid GRIP signature with string key`() {
        val signature = GripAuthHelper.createGripSignature(testIssuer, testKey)

        // Extract with correct key
        assertEquals(testIssuer, GripAuthHelper.extractIssuer(signature, testKey))

        // Extract with incorrect key
        assertNull(GripAuthHelper.extractIssuer(signature, "wrong-key-that-is-long-enough-for-hmac"))

        // Extract from invalid token
        assertNull(GripAuthHelper.extractIssuer("invalid-token", testKey))
    }

    @Test
    fun `should extract issuer from valid GRIP signature with SecretKey`() {
        val secretKey = Keys.hmacShaKeyFor(testKey.toByteArray(StandardCharsets.UTF_8))
        val wrongKey = Keys.hmacShaKeyFor("wrong-key-that-is-long-enough-for-hmac".toByteArray(StandardCharsets.UTF_8))
        val signature = GripAuthHelper.createGripSignature(testIssuer, secretKey)

        // Extract with correct key
        assertEquals(testIssuer, GripAuthHelper.extractIssuer(signature, secretKey))

        // Extract with incorrect key
        assertNull(GripAuthHelper.extractIssuer(signature, wrongKey))

        // Extract from invalid token
        assertNull(GripAuthHelper.extractIssuer("invalid-token", secretKey))
    }
}
