package io.github.mpecan.pmt.security.jwt.config

import io.github.mpecan.pmt.security.core.*
import io.github.mpecan.pmt.security.jwt.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Spring Boot AutoConfiguration for JWT functionality.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true"
    )
    fun jwtDecoderService(properties: JwtProperties): JwtDecoderService {
        return DefaultJwtDecoderService(properties)
    }
    
    @Bean
    @ConditionalOnMissingBean(JwtDecoderService::class)
    fun noOpJwtDecoderService(): JwtDecoderService {
        return NoOpJwtDecoderService()
    }
    
    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true"
    )
    fun claimExtractorService(): ClaimExtractorService {
        return JsonPathClaimExtractorService()
    }
    
    @Bean
    @ConditionalOnMissingBean(ClaimExtractorService::class)
    fun noOpClaimExtractorService(): ClaimExtractorService {
        return NoOpClaimExtractorService()
    }
    
    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true"
    )
    fun channelSubscriptionExtractorService(
        claimExtractorService: ClaimExtractorService,
        properties: JwtProperties
    ): ChannelSubscriptionExtractorService {
        return DefaultChannelSubscriptionExtractorService(claimExtractorService, properties)
    }
    
    @Bean
    @ConditionalOnMissingBean(ChannelSubscriptionExtractorService::class)
    fun noOpChannelSubscriptionExtractorService(): ChannelSubscriptionExtractorService {
        return NoOpChannelSubscriptionExtractorService()
    }
    
    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true"
    )
    fun jwtAuthenticationConverter(properties: JwtProperties): Converter<Jwt, AbstractAuthenticationToken> {
        return JwtAuthenticationConverterFactory(properties).createAuthenticationConverter()
    }
}