package io.github.mpecan.pmt.security.remote.config

import io.github.mpecan.pmt.security.remote.NoopRemoteAuthorizationClient
import io.github.mpecan.pmt.security.remote.RemoteAuthorizationClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * Auto-configuration for no-op remote authorization client when disabled.
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "pushpin.security.remote",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoOpRemoteAuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RemoteAuthorizationClient::class)
    fun noopRemoteAuthorizationClient(): RemoteAuthorizationClient {
        return NoopRemoteAuthorizationClient()
    }
}
