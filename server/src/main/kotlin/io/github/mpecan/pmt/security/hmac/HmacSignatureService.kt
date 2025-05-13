package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.config.PushpinProperties
import org.springframework.stereotype.Service
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service for generating and verifying HMAC signatures for server-to-server communication.
 */
@Service
class HmacSignatureService(private val properties: PushpinProperties) {
    
    /**
     * Generate an HMAC signature for the given data.
     */
    fun generateSignature(data: String): String {
        val secretKey = properties.security.hmac.secretKey
        val algorithm = properties.security.hmac.algorithm
        
        return try {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), algorithm)
            val mac = Mac.getInstance(algorithm)
            mac.init(keySpec)
            
            val hmacBytes = mac.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(hmacBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw HmacSignatureException("HMAC algorithm ${algorithm} not available", e)
        } catch (e: InvalidKeyException) {
            throw HmacSignatureException("Invalid HMAC key", e)
        }
    }
    
    /**
     * Verify an HMAC signature for the given data.
     */
    fun verifySignature(data: String, signature: String): Boolean {
        val calculatedSignature = generateSignature(data)
        return calculatedSignature == signature
    }
    
    /**
     * Generate a signature for a request body and headers.
     * 
     * @param body The request body
     * @param timestamp The request timestamp (for preventing replay attacks)
     * @param path The request path
     * @return The HMAC signature
     */
    fun generateRequestSignature(body: String, timestamp: Long, path: String): String {
        // Combine the data to sign
        val dataToSign = "$timestamp:$path:$body"
        return generateSignature(dataToSign)
    }
    
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
    ): Boolean {
        // Check if the request is too old (to prevent replay attacks)
        val currentTime = System.currentTimeMillis()
        if (currentTime - timestamp > maxAgeMs) {
            return false
        }
        
        // Verify the signature
        val dataToSign = "$timestamp:$path:$body"
        return verifySignature(dataToSign, signature)
    }
}

/**
 * Exception thrown when there is an error generating or verifying HMAC signatures.
 */
class HmacSignatureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)