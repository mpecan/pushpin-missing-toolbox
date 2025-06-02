package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.security.core.HmacService
import io.github.mpecan.pmt.security.core.HmacSignatureException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Default implementation of HmacService for generating and verifying HMAC signatures.
 */
class DefaultHmacService(
    private val properties: HmacProperties,
) : HmacService {
    override fun generateSignature(data: String): String {
        val secretKey = properties.secretKey
        val algorithm = properties.algorithm

        return try {
            val keySpec = SecretKeySpec(secretKey.toByteArray(), algorithm)
            val mac = Mac.getInstance(algorithm)
            mac.init(keySpec)

            val hmacBytes = mac.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(hmacBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw HmacSignatureException("HMAC algorithm $algorithm not available", e)
        } catch (e: InvalidKeyException) {
            throw HmacSignatureException("Invalid HMAC key", e)
        }
    }

    override fun verifySignature(
        data: String,
        signature: String,
    ): Boolean {
        val calculatedSignature = generateSignature(data)
        return calculatedSignature == signature
    }

    override fun generateRequestSignature(
        body: String,
        timestamp: Long,
        path: String,
    ): String {
        // Combine the data to sign
        val dataToSign = "$timestamp:$path:$body"
        return generateSignature(dataToSign)
    }

    override fun verifyRequestSignature(
        body: String,
        timestamp: Long,
        path: String,
        signature: String,
        maxAgeMs: Long,
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

    override fun isHmacEnabled(): Boolean = properties.enabled

    override fun getAlgorithm(): String = properties.algorithm

    override fun getHeaderName(): String = properties.headerName
}
