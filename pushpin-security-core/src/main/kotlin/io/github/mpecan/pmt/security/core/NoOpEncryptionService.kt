package io.github.mpecan.pmt.security.core

import java.util.Base64
import java.util.UUID

/**
 * No-op implementation of EncryptionService that passes data through without encryption.
 * This is the default implementation when no other EncryptionService bean is provided.
 * It ensures that code using EncryptionService will not fail if no encryption module is included.
 */
class NoOpEncryptionService : EncryptionService {

    override fun encrypt(plaintext: String): String {
        // Pass through without encryption
        return plaintext
    }

    override fun decrypt(encryptedData: String): String {
        // Pass through without decryption
        return encryptedData
    }

    override fun isEncryptionEnabled(): Boolean {
        return false
    }

    override fun generateSecretKey(): String {
        // Generate a dummy key that looks real but isn't used
        val randomBytes = UUID.randomUUID().toString().toByteArray()
        return Base64.getEncoder().encodeToString(randomBytes)
    }
}
