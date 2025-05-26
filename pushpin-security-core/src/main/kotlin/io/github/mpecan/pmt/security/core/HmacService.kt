package io.github.mpecan.pmt.security.core

/**
 * Service for generating and verifying HMAC signatures for secure communication.
 */
interface HmacService {
    /**
     * Generate an HMAC signature for the given data.
     */
    fun generateSignature(data: String): String
    
    /**
     * Verify an HMAC signature for the given data.
     */
    fun verifySignature(data: String, signature: String): Boolean
    
    /**
     * Generate a signature for a request body and headers.
     * 
     * @param body The request body
     * @param timestamp The request timestamp (for preventing replay attacks)
     * @param path The request path
     * @return The HMAC signature
     */
    fun generateRequestSignature(body: String, timestamp: Long, path: String): String
    
    /**
     * Verify a request signature.
     * 
     * @param body The request body
     * @param timestamp The request timestamp
     * @param path The request path
     * @param signature The provided signature to verify
     * @param maxAgeMs The maximum age of the request in milliseconds (to prevent replay attacks)
     * @return True if the signature is valid and the request is not too old
     */
    fun verifyRequestSignature(
        body: String, 
        timestamp: Long, 
        path: String, 
        signature: String,
        maxAgeMs: Long = 300000 // 5 minutes by default
    ): Boolean
    
    /**
     * Check if HMAC signing is enabled.
     */
    fun isHmacEnabled(): Boolean
    
    /**
     * Get the configured HMAC algorithm.
     */
    fun getAlgorithm(): String
    
    /**
     * Get the configured HMAC header name.
     */
    fun getHeaderName(): String
}

/**
 * Exception thrown when there is an error generating or verifying HMAC signatures.
 */
class HmacSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)