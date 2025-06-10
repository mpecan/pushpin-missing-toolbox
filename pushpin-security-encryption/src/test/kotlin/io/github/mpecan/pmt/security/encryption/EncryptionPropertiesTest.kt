package io.github.mpecan.pmt.security.encryption

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EncryptionPropertiesTest {
    @Test
    fun `should have default values`() {
        val properties = EncryptionProperties()

        assertFalse(properties.enabled)
        assertEquals("AES/GCM/NoPadding", properties.algorithm)
        assertEquals("", properties.secretKey)
        assertEquals(256, properties.keySize)
    }

    @Test
    fun `should allow custom values`() {
        val properties =
            EncryptionProperties(
                enabled = true,
                algorithm = "AES/CBC/PKCS5Padding",
                secretKey = "test-key",
                keySize = 128,
            )

        assertEquals(true, properties.enabled)
        assertEquals("AES/CBC/PKCS5Padding", properties.algorithm)
        assertEquals("test-key", properties.secretKey)
        assertEquals(128, properties.keySize)
    }

    @Test
    fun `should create copy with modified values`() {
        val original = EncryptionProperties()
        val modified = original.copy(enabled = true, secretKey = "new-key")

        assertFalse(original.enabled)
        assertEquals("", original.secretKey)
        assertEquals(true, modified.enabled)
        assertEquals("new-key", modified.secretKey)
        // Other values should remain the same
        assertEquals(original.algorithm, modified.algorithm)
        assertEquals(original.keySize, modified.keySize)
    }
}
