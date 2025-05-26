package io.github.mpecan.pmt.security.core

/**
 * No-op implementation of HmacService that is used when HMAC functionality is not available.
 */
class NoOpHmacService : HmacService {
    override fun generateSignature(data: String): String = ""

    override fun verifySignature(data: String, signature: String): Boolean = true

    override fun generateRequestSignature(body: String, timestamp: Long, path: String): String = ""

    override fun verifyRequestSignature(
        body: String,
        timestamp: Long,
        path: String,
        signature: String,
        maxAgeMs: Long,
    ): Boolean = true

    override fun isHmacEnabled(): Boolean = false

    override fun getAlgorithm(): String = "HmacSHA256"

    override fun getHeaderName(): String = "X-Pushpin-Signature"
}
