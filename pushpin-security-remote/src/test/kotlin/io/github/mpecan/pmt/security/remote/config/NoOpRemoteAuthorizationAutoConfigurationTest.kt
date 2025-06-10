package io.github.mpecan.pmt.security.remote.config

import io.github.mpecan.pmt.security.remote.NoopRemoteAuthorizationClient
import io.github.mpecan.pmt.security.remote.RemoteAuthorizationClient
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NoOpRemoteAuthorizationAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NoOpRemoteAuthorizationAutoConfiguration::class.java))

    @Test
    fun `should create NoopRemoteAuthorizationClient when remote is disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.remote.enabled=false")
            .run { context ->
                assertNotNull(context.getBean(RemoteAuthorizationClient::class.java))
                assertTrue(context.getBean(RemoteAuthorizationClient::class.java) is NoopRemoteAuthorizationClient)
            }
    }

    @Test
    fun `should create NoopRemoteAuthorizationClient when remote property is not set`() {
        contextRunner
            .run { context ->
                assertNotNull(context.getBean(RemoteAuthorizationClient::class.java))
                assertTrue(context.getBean(RemoteAuthorizationClient::class.java) is NoopRemoteAuthorizationClient)
            }
    }

    @Test
    fun `should not create NoopRemoteAuthorizationClient when remote is enabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.remote.enabled=true")
            .run { context ->
                assertTrue(context.getBeansOfType(RemoteAuthorizationClient::class.java).isEmpty())
            }
    }

    @Test
    fun `should not override existing RemoteAuthorizationClient bean`() {
        contextRunner
            .withPropertyValues("pushpin.security.remote.enabled=false")
            .withUserConfiguration(ExistingRemoteAuthorizationClientConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(RemoteAuthorizationClient::class.java))
                assertTrue(context.getBean(RemoteAuthorizationClient::class.java) is TestRemoteAuthorizationClient)
            }
    }

    @org.springframework.context.annotation.Configuration
    class ExistingRemoteAuthorizationClientConfiguration {
        @org.springframework.context.annotation.Bean
        fun remoteAuthorizationClient(): RemoteAuthorizationClient = TestRemoteAuthorizationClient()
    }

    class TestRemoteAuthorizationClient : RemoteAuthorizationClient {
        override fun canSubscribe(
            request: jakarta.servlet.http.HttpServletRequest,
            channelId: String,
        ): Boolean = true

        override fun getSubscribableChannels(request: jakarta.servlet.http.HttpServletRequest): List<String> =
            listOf("test")

        override fun getSubscribableChannelsByPattern(
            request: jakarta.servlet.http.HttpServletRequest,
            pattern: String,
        ): List<String> = listOf("test")
    }
}
