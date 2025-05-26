package io.github.mpecan.pmt.security.encryption

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultEncryptionServiceTest {
    
    private lateinit var encryptionService: DefaultEncryptionService
    private lateinit var properties: EncryptionProperties
    
    @BeforeEach
    fun setUp() {
        properties = EncryptionProperties(
            enabled = true,
            algorithm = "AES/GCM/NoPadding",
            secretKey = "", // Let the service generate a random key
            keySize = 256
        )
        encryptionService = DefaultEncryptionService(properties)
    }
    
    @Test
    fun `encrypt should encrypt plaintext`() {
        // Arrange
        val plaintext = "This is a secret message"
        
        // Act
        val ciphertext = encryptionService.encrypt(plaintext)
        
        // Assert
        assertNotEquals(plaintext, ciphertext)
        assertTrue(ciphertext.isNotEmpty())
    }
    
    @Test
    fun `decrypt should decrypt previously encrypted text`() {
        // Arrange
        val plaintext = "This is a secret message"
        val ciphertext = encryptionService.encrypt(plaintext)
        
        // Act
        val decrypted = encryptionService.decrypt(ciphertext)
        
        // Assert
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `encryption with disabled encryption should return original text`() {
        // Arrange
        val plaintext = "This is a message"
        val disabledProperties = EncryptionProperties(enabled = false)
        val disabledService = DefaultEncryptionService(disabledProperties)
        
        // Act
        val result = disabledService.encrypt(plaintext)
        
        // Assert
        assertEquals(plaintext, result)
    }
    
    @Test
    fun `decryption with disabled encryption should return original text`() {
        // Arrange
        val text = "This is not encrypted"
        val disabledProperties = EncryptionProperties(enabled = false)
        val disabledService = DefaultEncryptionService(disabledProperties)
        
        // Act
        val result = disabledService.decrypt(text)
        
        // Assert
        assertEquals(text, result)
    }
    
    @Test
    fun `generateSecretKey should return Base64 encoded key`() {
        // Act
        val key = encryptionService.generateSecretKey()
        
        // Assert
        assertNotNull(key)
        assertTrue(key.isNotEmpty())
        
        // Should be 44 chars for Base64-encoded 32-byte AES key
        assertTrue(key.length >= 40)
    }
    
    @Test
    fun `encrypt with blank input should return blank output`() {
        // Arrange
        val plaintext = ""
        
        // Act
        val result = encryptionService.encrypt(plaintext)
        
        // Assert
        assertEquals(plaintext, result)
    }
    
    @Test
    fun `decrypt with blank input should return blank output`() {
        // Arrange
        val ciphertext = ""
        
        // Act
        val result = encryptionService.decrypt(ciphertext)
        
        // Assert
        assertEquals(ciphertext, result)
    }
    
    @Test
    fun `encryption and decryption with configured secret key should work correctly`() {
        // Arrange
        val plaintext = "This is a secret message"
        val secretKey = encryptionService.generateSecretKey()
        
        // Create a new instance with a configured secret key
        val configuredProperties = EncryptionProperties(
            enabled = true,
            secretKey = secretKey
        )
        val configuredService = DefaultEncryptionService(configuredProperties)
        
        // Act
        val ciphertext = configuredService.encrypt(plaintext)
        val decrypted = configuredService.decrypt(ciphertext)
        
        // Assert
        assertNotEquals(plaintext, ciphertext)
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `isEncryptionEnabled should return correct value based on properties`() {
        // Assert enabled
        assertTrue(encryptionService.isEncryptionEnabled())
        
        // Test disabled
        val disabledProperties = EncryptionProperties(enabled = false)
        val disabledService = DefaultEncryptionService(disabledProperties)
        assertFalse(disabledService.isEncryptionEnabled())
    }
    
    @Test
    fun `decrypt with invalid data should throw EncryptionException`() {
        // Arrange
        val invalidData = "This is not valid encrypted data"
        
        // Act & Assert
        assertThrows<EncryptionException> {
            encryptionService.decrypt(invalidData)
        }
    }
    
    @Test
    fun `encryption should produce different ciphertext for same plaintext`() {
        // Arrange
        val plaintext = "Same message"
        
        // Act
        val ciphertext1 = encryptionService.encrypt(plaintext)
        val ciphertext2 = encryptionService.encrypt(plaintext)
        
        // Assert - Due to random IV, ciphertexts should be different
        assertNotEquals(ciphertext1, ciphertext2)
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(ciphertext1))
        assertEquals(plaintext, encryptionService.decrypt(ciphertext2))
    }
}