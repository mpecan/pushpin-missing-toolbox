package io.github.mpecan.pmt.security.jwt

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

/**
 * Creates JWT authentication converters based on configuration.
 */
class JwtAuthenticationConverterFactory(
    private val properties: JwtProperties,
) {

    /**
     * Creates a converter that extracts authorities from JWT claims.
     */
    fun createAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> {
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()

        // Configure the authorities claim name based on configuration
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName(
            properties.authoritiesClaim,
        )

        // Configure the authority prefix
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix(
            properties.authoritiesPrefix,
        )

        // Create and configure the authentication converter
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter)

        return jwtAuthenticationConverter
    }
}
