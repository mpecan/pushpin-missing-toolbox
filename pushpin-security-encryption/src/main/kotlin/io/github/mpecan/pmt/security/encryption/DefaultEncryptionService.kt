package io.github.mpecan.pmt.security.encryption

import io.github.mpecan.pmt.security.core.EncryptionService
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Default implementation of EncryptionService using AES/GCM for authenticated encryption.
 */
class DefaultEncryptionService(
    private val properties: EncryptionProperties,
) : EncryptionService {

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    // The secret key used for encryption, derived from the configured secret
    private val secretKey: SecretKey by lazy {
        if (properties.secretKey.isNotBlank()) {
            // Use the configured secret key
            val decodedKey = Base64.getDecoder().decode(properties.secretKey)
            SecretKeySpec(decodedKey, "AES")
        } else {
            // Generate a random key if none is provided
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(properties.keySize)
            keyGenerator.generateKey()
        }
    }

    /**
     * Encrypt data.
     *
     * @param plaintext The data to encrypt
     * @return The encrypted data, Base64-encoded
     */
    override fun encrypt(plaintext: String): String {
        if (!properties.enabled || plaintext.isBlank()) {
            return plaintext
        }

        try {
            // Generate a random IV
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            // Initialize the cipher
            val cipher = Cipher.getInstance(properties.algorithm)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            // Encrypt the data
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray())

            // Combine IV and encrypted data
            val result = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, result, iv.size, encryptedBytes.size)

            // Encode the result as Base64
            return Base64.getEncoder().encodeToString(result)
        } catch (e: Exception) {
            throw EncryptionException("Error encrypting data", e)
        }
    }

    /**
     * Decrypt data.
     *
     * @param encryptedData The encrypted data, Base64-encoded
     * @return The decrypted data
     */
    override fun decrypt(encryptedData: String): String {
        if (!properties.enabled || encryptedData.isBlank()) {
            return encryptedData
        }

        try {
            // Decode the Base64 data
            val encryptedBytes = Base64.getDecoder().decode(encryptedData)

            // Extract the IV
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(encryptedBytes, 0, iv, 0, iv.size)

            // Extract the encrypted data
            val ciphertext = ByteArray(encryptedBytes.size - iv.size)
            System.arraycopy(encryptedBytes, iv.size, ciphertext, 0, ciphertext.size)

            // Initialize the cipher
            val cipher = Cipher.getInstance(properties.algorithm)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            // Decrypt the data
            val decryptedBytes = cipher.doFinal(ciphertext)

            // Convert to string
            return String(decryptedBytes)
        } catch (e: Exception) {
            throw EncryptionException("Error decrypting data", e)
        }
    }

    /**
     * Check if encryption is enabled.
     *
     * @return true if encryption is enabled, false otherwise
     */
    override fun isEncryptionEnabled(): Boolean {
        return properties.enabled
    }

    /**
     * Generate a new random secret key for encryption.
     * * @return The Base64-encoded secret key
     */
    override fun generateSecretKey(): String {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(properties.keySize)
        val key = keyGenerator.generateKey()
        return Base64.getEncoder().encodeToString(key.encoded)
    }
}
