package io.github.mpecan.pmt.security.remote.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.remote.HttpRemoteSubscriptionClient
import io.github.mpecan.pmt.security.remote.RemoteAuthorizationClient
import io.github.mpecan.pmt.security.remote.RemoteAuthorizationProperties
import io.github.mpecan.pmt.security.remote.SubscriptionAuthorizationCache
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Spring Boot auto-configuration for remote authorization.
 */
@AutoConfiguration
@EnableConfigurationProperties(RemoteAuthorizationProperties::class)
@ConditionalOnProperty(
    prefix = "pushpin.security.remote",
    name = ["enabled"],
    havingValue = "true",
)
class RemoteAuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun remoteAuthorizationRestTemplate(
        properties: RemoteAuthorizationProperties,
        builder: RestTemplateBuilder,
    ): RestTemplate {
        return builder
            .connectTimeout(Duration.ofMillis(properties.timeout))
            .readTimeout(Duration.ofMillis(properties.timeout))
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun subscriptionAuthorizationCache(properties: RemoteAuthorizationProperties): SubscriptionAuthorizationCache {
        return SubscriptionAuthorizationCache(
            cacheMaxSize = properties.cache.maxSize,
            cacheTtl = properties.cache.ttl,
        )
    }

    @Bean
    @ConditionalOnMissingBean(RemoteAuthorizationClient::class)
    fun remoteAuthorizationClient(
        properties: RemoteAuthorizationProperties,
        cache: SubscriptionAuthorizationCache,
        restTemplate: RestTemplate,
        auditService: AuditService,
    ): RemoteAuthorizationClient {
        return HttpRemoteSubscriptionClient(properties, cache, restTemplate, auditService)
    }
}
