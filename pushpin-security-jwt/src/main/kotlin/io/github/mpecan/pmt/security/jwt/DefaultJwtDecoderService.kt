package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.security.core.JwtDecoderService
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.*
import javax.crypto.spec.SecretKeySpec

/**
 * Default implementation of JwtDecoderService for providing JWT decoders.
 */
class DefaultJwtDecoderService(
    private val properties: JwtProperties,
) : JwtDecoderService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getDecoder(): JwtDecoder {
        logger.info("Configuring JWT decoder for provider: ${properties.provider}")

        val jwtDecoder = when (properties.provider.lowercase()) {
            "keycloak", "auth0", "okta", "oauth2" -> {
                if (properties.jwksUri.isBlank()) {
                    throw IllegalStateException("JWKS URI must be provided for provider: ${properties.provider}")
                }

                logger.info("Configuring JWT decoder with JWKS URI: ${properties.jwksUri}")
                NimbusJwtDecoder.withJwkSetUri(properties.jwksUri)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build()
            }
            "symmetric" -> {
                if (properties.secret.length < 32) {
                    throw IllegalStateException("Secret key must be at least 32 characters long")
                }

                logger.info("Configuring JWT decoder with symmetric key")
                val secretKey = SecretKeySpec(
                    properties.secret.toByteArray(),
                    "HMAC",
                )

                NimbusJwtDecoder.withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build()
            }
            else -> {
                throw IllegalStateException("Unknown JWT provider: ${properties.provider}")
            }
        }

        // Add validators based on configuration
        val validators = mutableListOf<OAuth2TokenValidator<Jwt>>()

        // Always validate timestamps (expiration, not before)
        validators.add(JwtTimestampValidator())

        // Validate issuer if configured
        if (properties.issuer.isNotBlank()) {
            validators.add(
                JwtClaimValidator<String>(JwtClaimNames.ISS) {
                        iss ->
                    iss == properties.issuer
                },
            )
        }

        // Validate audience if configured
        if (properties.audience.isNotBlank()) {
            validators.add(
                JwtClaimValidator<Any>(JwtClaimNames.AUD) {
                        aud ->
                    aud is List<*> && properties.audience in aud ||
                        aud == properties.audience
                },
            )
        }

        if (validators.size > 1) {
            jwtDecoder.setJwtValidator(DelegatingOAuth2TokenValidator(validators))
        }

        return jwtDecoder
    }

    override fun isJwtEnabled(): Boolean = properties.enabled

    override fun getProvider(): String = properties.provider

    override fun getIssuer(): String = properties.issuer

    override fun getAudience(): String = properties.audience
}
