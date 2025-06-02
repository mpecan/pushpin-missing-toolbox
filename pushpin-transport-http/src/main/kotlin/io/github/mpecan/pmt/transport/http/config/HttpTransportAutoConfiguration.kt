package io.github.mpecan.pmt.transport.http.config

import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.transport.http.HttpHealthChecker
import io.github.mpecan.pmt.transport.http.HttpTransport
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

/**
 * Auto-configuration for HTTP transport components.
 */
@AutoConfiguration
@ConditionalOnProperty(value = ["pushpin.transport"], havingValue = "http", matchIfMissing = true)
class HttpTransportAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun webClient(): WebClient = WebClient.builder().build()

    @Bean
    @ConditionalOnMissingBean
    fun httpTransport(
        webClient: WebClient,
        messageSerializer: MessageSerializer,
        discoveryManager: PushpinDiscoveryManager,
    ): HttpTransport = HttpTransport(webClient, messageSerializer, discoveryManager)

    @Bean
    @ConditionalOnMissingBean
    fun httpHealthChecker(webClient: WebClient): HttpHealthChecker = HttpHealthChecker(webClient)
}
