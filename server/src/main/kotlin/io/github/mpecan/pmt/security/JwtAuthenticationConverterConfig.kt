package io.github.mpecan.pmt.security

import io.github.mpecan.pmt.config.PushpinProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

/**
 * Configuration for converting JWT claims to Spring Security authorities.
 */
@Configuration
class JwtAuthenticationConverterConfig(private val pushpinProperties: PushpinProperties) {
    
    /**
     * Creates a converter that extracts authorities from JWT claims.
     */
    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> {
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        
        // Configure the authorities claim name based on configuration
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName(
            pushpinProperties.security.jwt.authoritiesClaim
        )
        
        // Configure the authority prefix
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix(
            pushpinProperties.security.jwt.authoritiesPrefix
        )
        
        // Create and configure the authentication converter
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter)
        
        return jwtAuthenticationConverter
    }
}