package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.config.PushpinProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class HmacSignatureServiceTest {
    
    private lateinit var hmacSignatureService: HmacSignatureService
    private lateinit var pushpinProperties: PushpinProperties
    
    @BeforeEach
    fun setUp() {
        // Create mock properties
        val hmacProperties = PushpinProperties.HmacProperties(
            enabled = true,
            algorithm = "HmacSHA256",
            secretKey = "test-hmac-secret",
            headerName = "X-Pushpin-Signature"
        )
        
        val securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { hmac } doReturn hmacProperties
        }
        
        pushpinProperties = mock<PushpinProperties> {
            on { security } doReturn securityProperties
        }
        
        hmacSignatureService = HmacSignatureService(pushpinProperties)
    }
    
    @Test
    fun `generateSignature should produce a signature`() {
        // Arrange
        val data = "Test data to sign"
        
        // Act
        val signature = hmacSignatureService.generateSignature(data)
        
        // Assert
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }
    
    @Test
    fun `verifySignature should return true for valid signature`() {
        // Arrange
        val data = "Test data to sign"
        val signature = hmacSignatureService.generateSignature(data)
        
        // Act
        val isValid = hmacSignatureService.verifySignature(data, signature)
        
        // Assert
        assertTrue(isValid)
    }
    
    @Test
    fun `verifySignature should return false for invalid signature`() {
        // Arrange
        val data = "Test data to sign"
        val invalidSignature = "invalid-signature"
        
        // Act
        val isValid = hmacSignatureService.verifySignature(data, invalidSignature)
        
        // Assert
        assertFalse(isValid)
    }
    
    @Test
    fun `generateRequestSignature should include all parameters`() {
        // Arrange
        val body = "Request body"
        val timestamp = 123456789L
        val path = "/api/endpoint"
        
        // Act
        val signature1 = hmacSignatureService.generateRequestSignature(body, timestamp, path)
        
        // Change one parameter and generate again
        val signature2 = hmacSignatureService.generateRequestSignature(body, timestamp + 1, path)
        
        // Assert
        assertNotEquals(signature1, signature2, "Signatures should be different when parameters change")
    }
    
    @Test
    fun `verifyRequestSignature should return true for valid request`() {
        // Arrange
        val body = "Request body"
        val timestamp = System.currentTimeMillis()
        val path = "/api/endpoint"
        val signature = hmacSignatureService.generateRequestSignature(body, timestamp, path)
        
        // Act
        val isValid = hmacSignatureService.verifyRequestSignature(body, timestamp, path, signature)
        
        // Assert
        assertTrue(isValid)
    }
    
    @Test
    fun `verifyRequestSignature should return false for expired request`() {
        // Arrange
        val body = "Request body"
        val timestamp = System.currentTimeMillis() - 600000 // 10 minutes ago (beyond max age)
        val path = "/api/endpoint"
        val signature = hmacSignatureService.generateRequestSignature(body, timestamp, path)
        
        // Act
        val isValid = hmacSignatureService.verifyRequestSignature(body, timestamp, path, signature, 300000)
        
        // Assert
        assertFalse(isValid, "Should reject expired requests")
    }
    
    @Test
    fun `verifyRequestSignature should return false for tampered request`() {
        // Arrange
        val body = "Original body"
        val timestamp = System.currentTimeMillis()
        val path = "/api/endpoint"
        val signature = hmacSignatureService.generateRequestSignature(body, timestamp, path)
        
        // Act - try to verify with a different body
        val isValid = hmacSignatureService.verifyRequestSignature("Tampered body", timestamp, path, signature)
        
        // Assert
        assertFalse(isValid, "Should reject tampered requests")
    }
}