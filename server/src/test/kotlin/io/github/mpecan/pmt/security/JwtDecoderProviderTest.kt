package io.github.mpecan.pmt.security

import io.github.mpecan.pmt.config.PushpinProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

class JwtDecoderProviderTest {
    
    private val baseProperties = PushpinProperties(
        security = PushpinProperties.SecurityProperties(
            jwt = PushpinProperties.JwtProperties(
                enabled = true,
                secret = "a-very-secure-secret-key-that-is-at-least-32-chars",
                provider = "symmetric"
            )
        )
    )
    
    @Test
    fun `getDecoder should create decoder for symmetric provider`() {
        val properties = baseProperties.copy(
            security = baseProperties.security.copy(
                jwt = baseProperties.security.jwt.copy(
                    provider = "symmetric"
                )
            )
        )
        
        val provider = JwtDecoderProvider(properties)
        val decoder = provider.getDecoder()
        
        assert(decoder is NimbusJwtDecoder)
    }
    
    @Test
    fun `getDecoder should throw exception for invalid provider`() {
        val properties = baseProperties.copy(
            security = baseProperties.security.copy(
                jwt = baseProperties.security.jwt.copy(
                    provider = "invalid-provider"
                )
            )
        )
        
        val provider = JwtDecoderProvider(properties)
        
        assertThrows<IllegalStateException> {
            provider.getDecoder()
        }
    }
    
    @Test
    fun `getDecoder should throw exception for missing JWKS URI with external provider`() {
        val properties = baseProperties.copy(
            security = baseProperties.security.copy(
                jwt = baseProperties.security.jwt.copy(
                    provider = "keycloak",
                    jwksUri = ""
                )
            )
        )
        
        val provider = JwtDecoderProvider(properties)
        
        assertThrows<IllegalStateException> {
            provider.getDecoder()
        }
    }
    
    @Test
    fun `getDecoder should throw exception for too short secret key with symmetric provider`() {
        val properties = baseProperties.copy(
            security = baseProperties.security.copy(
                jwt = baseProperties.security.jwt.copy(
                    provider = "symmetric",
                    secret = "too-short"
                )
            )
        )
        
        val provider = JwtDecoderProvider(properties)
        
        assertThrows<IllegalStateException> {
            provider.getDecoder()
        }
    }
}