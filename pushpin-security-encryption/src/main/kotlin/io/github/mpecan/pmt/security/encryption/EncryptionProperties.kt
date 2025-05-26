package io.github.mpecan.pmt.security.encryption

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for encryption functionality.
 */
@ConfigurationProperties(prefix = "pushpin.security.encryption")
data class EncryptionProperties(
    /**
     * Whether encryption is enabled.
     */
    val enabled: Boolean = false,
    
    /**
     * The encryption algorithm to use.
     * Default is AES/GCM/NoPadding for authenticated encryption.
     */
    val algorithm: String = "AES/GCM/NoPadding",
    
    /**
     * The Base64-encoded secret key to use for encryption.
     * If not provided, a random key will be generated (not recommended for production).
     */
    val secretKey: String = "",
    
    /**
     * The key size in bits.
     * Default is 256 for AES-256.
     */
    val keySize: Int = 256
)