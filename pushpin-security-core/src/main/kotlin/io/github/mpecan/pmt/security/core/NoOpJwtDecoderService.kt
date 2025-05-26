package io.github.mpecan.pmt.security.core

import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import javax.crypto.spec.SecretKeySpec

/**
 * No-op implementation of JwtDecoderService that is used when JWT functionality is not available.
 */
class NoOpJwtDecoderService : JwtDecoderService {
    override fun getDecoder(): JwtDecoder {
        // Return a simple decoder with a default secret to avoid configuration errors
        val secretKey = SecretKeySpec("default-secret-key-32-characters".toByteArray(), "HMAC")
        return NimbusJwtDecoder.withSecretKey(secretKey).build()
    }

    override fun isJwtEnabled(): Boolean = false

    override fun getProvider(): String = "none"

    override fun getIssuer(): String = ""

    override fun getAudience(): String = ""
}
