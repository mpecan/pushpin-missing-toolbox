package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.config.PushpinProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier

class ConfigurationBasedDiscoveryTest {

    private val pushpinProperties: PushpinProperties = mock()

    @Test
    fun `should discover servers from configuration`() {
        // Given
        val configDiscoveryProps = ConfigurationDiscoveryProperties(enabled = true)
        val serverProps1 = PushpinProperties.ServerProperties(
            id = "server1",
            host = "localhost",
            port = 7999,
            active = true
        )
        val serverProps2 = PushpinProperties.ServerProperties(
            id = "server2",
            host = "localhost",
            port = 8000,
            active = true
        )
        val inactiveServerProps = PushpinProperties.ServerProperties(
            id = "inactive",
            host = "localhost",
            port = 8001,
            active = false
        )

        whenever(pushpinProperties.servers).thenReturn(listOf(serverProps1, serverProps2, inactiveServerProps))

        val discovery = ConfigurationBasedDiscovery(configDiscoveryProps, pushpinProperties)

        // When
        val serversFlux = discovery.discoverServers()

        // Then
        StepVerifier.create(serversFlux)
            .expectNextMatches { server -> server.id == "server1" && server.port == 7999 }
            .expectNextMatches { server -> server.id == "server2" && server.port == 8000 }
            .verifyComplete()
    }

    @Test
    fun `should return empty flux when no active servers`() {
        // Given
        val configDiscoveryProps = ConfigurationDiscoveryProperties(enabled = true)
        val inactiveServerProps = PushpinProperties.ServerProperties(
            id = "inactive",
            host = "localhost",
            port = 8001,
            active = false
        )

        whenever(pushpinProperties.servers).thenReturn(listOf(inactiveServerProps))

        val discovery = ConfigurationBasedDiscovery(configDiscoveryProps, pushpinProperties)

        // When
        val serversFlux = discovery.discoverServers()

        // Then
        StepVerifier.create(serversFlux)
            .verifyComplete()
    }

    @Test
    fun `should return empty flux when no servers configured`() {
        // Given
        val configDiscoveryProps = ConfigurationDiscoveryProperties(enabled = true)

        whenever(pushpinProperties.servers).thenReturn(emptyList())

        val discovery = ConfigurationBasedDiscovery(configDiscoveryProps, pushpinProperties)

        // When
        val serversFlux = discovery.discoverServers()

        // Then
        StepVerifier.create(serversFlux)
            .verifyComplete()
    }

    @Test
    fun `isEnabled should return property value`() {
        // Given
        val enabledProps = ConfigurationDiscoveryProperties(enabled = true)
        val disabledProps = ConfigurationDiscoveryProperties(enabled = false)

        val enabledDiscovery = ConfigurationBasedDiscovery(enabledProps, pushpinProperties)
        val disabledDiscovery = ConfigurationBasedDiscovery(disabledProps, pushpinProperties)

        // Then
        assert(enabledDiscovery.isEnabled())
        assert(!disabledDiscovery.isEnabled())
    }
}
