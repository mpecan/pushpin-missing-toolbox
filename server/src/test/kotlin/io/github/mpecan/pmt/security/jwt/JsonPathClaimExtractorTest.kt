package io.github.mpecan.pmt.security.jwt

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonPathClaimExtractorTest {

    private lateinit var jsonPathClaimExtractor: JsonPathClaimExtractor
    private lateinit var mockJwt: Jwt

    @BeforeEach
    fun setUp() {
        jsonPathClaimExtractor = JsonPathClaimExtractor()
        
        // Create a mock JWT token with test claims
        val headers = mapOf(
            "alg" to "HS256",
            "typ" to "JWT"
        )
        
        val claims = mapOf(
            "sub" to "testuser",
            "name" to "Test User",
            "roles" to listOf("ROLE_USER", "ROLE_ADMIN"),
            "channels" to mapOf(
                "channel1" to listOf("READ", "WRITE"),
                "channel2" to listOf("READ")
            ),
            "permissions" to listOf(
                mapOf(
                    "id" to "channel3",
                    "permissions" to listOf("READ", "WRITE", "ADMIN")
                ),
                mapOf(
                    "id" to "channel4",
                    "permissions" to listOf("READ")
                )
            ),
            "flatChannels" to listOf(
                "channel5:READ",
                "channel5:WRITE",
                "channel6:READ"
            ),
            "numberValue" to 123,
            "booleanValue" to true,
            "nestedObject" to mapOf(
                "key1" to "value1",
                "key2" to mapOf(
                    "nestedKey" to "nestedValue"
                )
            )
        )
        
        mockJwt = Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            headers,
            claims
        )
    }

    @Test
    fun `extractStringClaim should return string value for valid path`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "$.sub")
        
        // Assert
        assertEquals("testuser", result)
    }
    
    @Test
    fun `extractStringClaim should handle direct claim name without $ prefix`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "name")
        
        // Assert
        assertEquals("Test User", result)
    }
    
    @Test
    fun `extractStringClaim should return null for non-existent path`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "$.nonExistentClaim")
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `extractStringClaim should convert non-string values to string`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "$.numberValue")
        
        // Assert
        assertEquals("123", result)
    }
    
    @Test
    fun `extractStringClaim should handle nested paths`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "$.nestedObject.key1")
        
        // Assert
        assertEquals("value1", result)
    }
    
    @Test
    fun `extractStringClaim should handle deeply nested paths`() {
        // Act
        val result = jsonPathClaimExtractor.extractStringClaim(mockJwt, "$.nestedObject.key2.nestedKey")
        
        // Assert
        assertEquals("nestedValue", result)
    }
    
    @Test
    fun `extractListClaim should return list for array path`() {
        // Act
        val result = jsonPathClaimExtractor.extractListClaim(mockJwt, "$.roles")
        
        // Assert
        assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), result)
    }
    
    @Test
    fun `extractListClaim should convert single value to single-item list`() {
        // Act
        val result = jsonPathClaimExtractor.extractListClaim(mockJwt, "$.sub")
        
        // Assert
        assertEquals(listOf("testuser"), result)
    }
    
    @Test
    fun `extractListClaim should return empty list for non-existent path`() {
        // Act
        val result = jsonPathClaimExtractor.extractListClaim(mockJwt, "$.nonExistentClaim")
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `extractMapClaim should return map for object path`() {
        // Act
        val result = jsonPathClaimExtractor.extractMapClaim(mockJwt, "$.channels")
        
        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsKey("channel1"))
        assertTrue(result.containsKey("channel2"))
    }
    
    @Test
    fun `extractMapClaim should return empty map for non-map values`() {
        // Act
        val result = jsonPathClaimExtractor.extractMapClaim(mockJwt, "$.sub")
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `extractMapClaim should return empty map for non-existent path`() {
        // Act
        val result = jsonPathClaimExtractor.extractMapClaim(mockJwt, "$.nonExistentClaim")
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `hasClaim should return true for existing path`() {
        // Act
        val result = jsonPathClaimExtractor.hasClaim(mockJwt, "$.sub")
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun `hasClaim should return false for non-existent path`() {
        // Act
        val result = jsonPathClaimExtractor.hasClaim(mockJwt, "$.nonExistentClaim")
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun `hasClaim should handle direct claim name without $ prefix`() {
        // Act
        val result = jsonPathClaimExtractor.hasClaim(mockJwt, "name")
        
        // Assert
        assertTrue(result)
    }
}