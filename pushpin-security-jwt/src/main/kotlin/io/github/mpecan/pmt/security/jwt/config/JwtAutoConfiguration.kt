package io.github.mpecan.pmt.security.jwt.config

import io.github.mpecan.pmt.security.core.ChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.core.ClaimExtractorService
import io.github.mpecan.pmt.security.core.JwtDecoderService
import io.github.mpecan.pmt.security.core.NoOpChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.core.NoOpClaimExtractorService
import io.github.mpecan.pmt.security.core.NoOpJwtDecoderService
import io.github.mpecan.pmt.security.jwt.DefaultChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.jwt.DefaultJwtDecoderService
import io.github.mpecan.pmt.security.jwt.JsonPathClaimExtractorService
import io.github.mpecan.pmt.security.jwt.JwtAuthenticationConverterFactory
import io.github.mpecan.pmt.security.jwt.JwtProperties
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
        havingValue = "true",
    )
    fun jwtDecoderService(properties: JwtProperties): JwtDecoderService = DefaultJwtDecoderService(properties)

    @Bean
    @ConditionalOnMissingBean(JwtDecoderService::class)
    fun noOpJwtDecoderService(): JwtDecoderService = NoOpJwtDecoderService()

    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true",
    )
    fun claimExtractorService(): ClaimExtractorService = JsonPathClaimExtractorService()

    @Bean
    @ConditionalOnMissingBean(ClaimExtractorService::class)
    fun noOpClaimExtractorService(): ClaimExtractorService = NoOpClaimExtractorService()

    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true",
    )
    fun channelSubscriptionExtractorService(
        claimExtractorService: ClaimExtractorService,
        properties: JwtProperties,
    ): ChannelSubscriptionExtractorService =
        DefaultChannelSubscriptionExtractorService(claimExtractorService, properties)

    @Bean
    @ConditionalOnMissingBean(ChannelSubscriptionExtractorService::class)
    fun noOpChannelSubscriptionExtractorService(): ChannelSubscriptionExtractorService =
        NoOpChannelSubscriptionExtractorService()

    @Bean
    @ConditionalOnProperty(
        prefix = "pushpin.security.jwt",
        name = ["enabled"],
        havingValue = "true",
    )
    fun jwtAuthenticationConverter(properties: JwtProperties): Converter<Jwt, AbstractAuthenticationToken> =
        JwtAuthenticationConverterFactory(properties).createAuthenticationConverter()
}
