package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NoOpEncryptionServiceTest {
    private lateinit var encryptionService: NoOpEncryptionService

    @BeforeEach
    fun setUp() {
        encryptionService = NoOpEncryptionService()
    }

    @Test
    fun `encrypt should return original text`() {
        // Arrange
        val plaintext = "This should not be encrypted"

        // Act
        val result = encryptionService.encrypt(plaintext)

        // Assert
        assertEquals(plaintext, result)
    }

    @Test
    fun `decrypt should return original text`() {
        // Arrange
        val encryptedData = "This should not be decrypted"

        // Act
        val result = encryptionService.decrypt(encryptedData)

        // Assert
        assertEquals(encryptedData, result)
    }

    @Test
    fun `isEncryptionEnabled should return false`() {
        // Act & Assert
        assertFalse(encryptionService.isEncryptionEnabled())
    }

    @Test
    fun `generateSecretKey should return a Base64 string`() {
        // Act
        val key = encryptionService.generateSecretKey()

        // Assert
        assertNotNull(key)
        assertTrue(key.isNotEmpty())

        // Verify it's valid Base64
        assertDoesNotThrow {
            java.util.Base64
                .getDecoder()
                .decode(key)
        }
    }

    @Test
    fun `encrypt and decrypt with empty strings should work`() {
        // Arrange
        val empty = ""

        // Act
        val encrypted = encryptionService.encrypt(empty)
        val decrypted = encryptionService.decrypt(empty)

        // Assert
        assertEquals(empty, encrypted)
        assertEquals(empty, decrypted)
    }
}
