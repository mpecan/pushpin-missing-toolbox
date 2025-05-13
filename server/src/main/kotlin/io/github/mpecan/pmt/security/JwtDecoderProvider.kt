package io.github.mpecan.pmt.security

import io.github.mpecan.pmt.config.PushpinProperties
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.security.interfaces.RSAPublicKey
import javax.crypto.SecretKey
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * Provider for JWT decoder that supports different JWT providers.
 */
@Service
class JwtDecoderProvider(private val pushpinProperties: PushpinProperties) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Get the appropriate JWT decoder based on configuration.
     */
    fun getDecoder(): JwtDecoder {
        logger.info("Configuring JWT decoder for provider: ${pushpinProperties.security.jwt.provider}")
        
        val jwtDecoder = when (pushpinProperties.security.jwt.provider.lowercase()) {
            "keycloak", "auth0", "okta", "oauth2" -> {
                if (pushpinProperties.security.jwt.jwksUri.isBlank()) {
                    throw IllegalStateException("JWKS URI must be provided for provider: ${pushpinProperties.security.jwt.provider}")
                }
                
                logger.info("Configuring JWT decoder with JWKS URI: ${pushpinProperties.security.jwt.jwksUri}")
                NimbusJwtDecoder.withJwkSetUri(pushpinProperties.security.jwt.jwksUri)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build()
            }
            "symmetric" -> {
                if (pushpinProperties.security.jwt.secret.length < 32) {
                    throw IllegalStateException("Secret key must be at least 32 characters long")
                }
                
                logger.info("Configuring JWT decoder with symmetric key")
                val secretKey = SecretKeySpec(
                    pushpinProperties.security.jwt.secret.toByteArray(),
                    "HMAC"
                )
                
                NimbusJwtDecoder.withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build()
            }
            else -> {
                throw IllegalStateException("Unknown JWT provider: ${pushpinProperties.security.jwt.provider}")
            }
        }
        
        // Add validators based on configuration
        val validators = mutableListOf<OAuth2TokenValidator<Jwt>>()
        
        // Always validate timestamps (expiration, not before)
        validators.add(JwtTimestampValidator())
        
        // Validate issuer if configured
        if (pushpinProperties.security.jwt.issuer.isNotBlank()) {
            validators.add(JwtClaimValidator<String>(JwtClaimNames.ISS) {
                iss -> iss == pushpinProperties.security.jwt.issuer
            })
        }

        // Validate audience if configured
        if (pushpinProperties.security.jwt.audience.isNotBlank()) {
            validators.add(JwtClaimValidator<Any>(JwtClaimNames.AUD) {
                aud -> aud is List<*> && pushpinProperties.security.jwt.audience in aud
                    || aud == pushpinProperties.security.jwt.audience
            })
        }
        
        if (validators.size > 1) {
            jwtDecoder.setJwtValidator(DelegatingOAuth2TokenValidator(validators))
        }
        
        return jwtDecoder
    }
}