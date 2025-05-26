package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.config.PushpinProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Configuration for remote authorization.
 */
@Configuration
@ConditionalOnProperty(name = ["pushpin.security.jwt.remoteAuthorization.enabled"], havingValue = "true")
class RemoteAuthorizationConfig(private val properties: PushpinProperties) {
    
    /**
     * Create a RestTemplate configured for remote authorization requests.
     */
    @Bean("authorizationRestTemplate")
    fun authorizationRestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(properties.security.jwt.remoteAuthorization.timeout))
            .setReadTimeout(Duration.ofMillis(properties.security.jwt.remoteAuthorization.timeout))
            .build()
    }
}