package io.github.mpecan.pmt.security.core

/**
 * Core interface for encryption services.
 * Implementations can provide different encryption strategies for securing
 * sensitive channel data.
 */
interface EncryptionService {
    /**
     * Encrypt data.
     *
     * @param plaintext The data to encrypt
     * @return The encrypted data, typically Base64-encoded
     */
    fun encrypt(plaintext: String): String
    
    /**
     * Decrypt data.
     *
     * @param encryptedData The encrypted data, typically Base64-encoded
     * @return The decrypted data
     */
    fun decrypt(encryptedData: String): String
    
    /**
     * Check if encryption is enabled.
     *
     * @return true if encryption is enabled, false otherwise
     */
    fun isEncryptionEnabled(): Boolean
    
    /**
     * Generate a new random secret key for encryption.
     * 
     * @return The Base64-encoded secret key
     */
    fun generateSecretKey(): String
}