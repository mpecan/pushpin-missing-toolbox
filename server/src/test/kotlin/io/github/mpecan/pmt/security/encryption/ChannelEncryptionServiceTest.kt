package io.github.mpecan.pmt.security.encryption

import io.github.mpecan.pmt.config.PushpinProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChannelEncryptionServiceTest {
    
    private lateinit var channelEncryptionService: ChannelEncryptionService
    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var encryptionProperties: PushpinProperties.EncryptionProperties
    private lateinit var securityProperties: PushpinProperties.SecurityProperties
    
    @BeforeEach
    fun setUp() {
        // Create mock properties
        encryptionProperties = mock<PushpinProperties.EncryptionProperties> {
            on { enabled } doReturn true
            on { algorithm } doReturn "AES/GCM/NoPadding"
            on { secretKey } doReturn ""  // Let the service generate a random key
        }
        
        securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { encryption } doReturn encryptionProperties
        }
        
        pushpinProperties = mock<PushpinProperties> {
            on { security } doReturn securityProperties
        }
        
        channelEncryptionService = ChannelEncryptionService(pushpinProperties)
    }
    
    @Test
    fun `encrypt should encrypt plaintext`() {
        // Arrange
        val plaintext = "This is a secret message"
        
        // Act
        val ciphertext = channelEncryptionService.encrypt(plaintext)
        
        // Assert
        assertNotEquals(plaintext, ciphertext)
        assertTrue(ciphertext.isNotEmpty())
    }
    
    @Test
    fun `decrypt should decrypt previously encrypted text`() {
        // Arrange
        val plaintext = "This is a secret message"
        val ciphertext = channelEncryptionService.encrypt(plaintext)
        
        // Act
        val decrypted = channelEncryptionService.decrypt(ciphertext)
        
        // Assert
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `encryption with disabled encryption should return original text`() {
        // Arrange
        val plaintext = "This is a message"
        
        // Modify mock to disable encryption
        whenever(encryptionProperties.enabled).thenReturn(false)
        
        // Act
        val result = channelEncryptionService.encrypt(plaintext)
        
        // Assert
        assertEquals(plaintext, result)
    }
    
    @Test
    fun `decryption with disabled encryption should return original text`() {
        // Arrange
        val text = "This is not encrypted"
        
        // Modify mock to disable encryption
        whenever(encryptionProperties.enabled).thenReturn(false)
        
        // Act
        val result = channelEncryptionService.decrypt(text)
        
        // Assert
        assertEquals(text, result)
    }
    
    @Test
    fun `generateSecretKey should return Base64 encoded key`() {
        // Act
        val key = channelEncryptionService.generateSecretKey()
        
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
        val result = channelEncryptionService.encrypt(plaintext)
        
        // Assert
        assertEquals(plaintext, result)
    }
    
    @Test
    fun `decrypt with blank input should return blank output`() {
        // Arrange
        val ciphertext = ""
        
        // Act
        val result = channelEncryptionService.decrypt(ciphertext)
        
        // Assert
        assertEquals(ciphertext, result)
    }
    
    @Test
    fun `encryption and decryption with configured secret key should work correctly`() {
        // Arrange
        val plaintext = "This is a secret message"
        val secretKey = channelEncryptionService.generateSecretKey()
        
        // Create a new instance with a configured secret key
        whenever(encryptionProperties.secretKey).thenReturn(secretKey)
        val encryptionService = ChannelEncryptionService(pushpinProperties)
        
        // Act
        val ciphertext = encryptionService.encrypt(plaintext)
        val decrypted = encryptionService.decrypt(ciphertext)
        
        // Assert
        assertNotEquals(plaintext, ciphertext)
        assertEquals(plaintext, decrypted)
    }
}