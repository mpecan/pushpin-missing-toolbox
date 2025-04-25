package io.github.mpecan.pmt.discovery

import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import java.time.Duration

class PushpinDiscoveryManagerTest {

    private val discoveryProperties: DiscoveryProperties = mock()
    private val discovery1: PushpinDiscovery = mock()
    private val discovery2: PushpinDiscovery = mock()

    private lateinit var manager: PushpinDiscoveryManager

    @BeforeEach
    fun setup() {
        whenever(discoveryProperties.enabled).thenReturn(true)

        manager = PushpinDiscoveryManager(discoveryProperties, listOf(discovery1, discovery2))
    }

    @Test
    fun `should initialize and refresh servers on startup`() {
        // Given
        val server1 = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 7999
        )
        val server2 = PushpinServer(
            id = "server2",
            host = "localhost",
            port = 8000
        )
        whenever(discoveryProperties.refreshInterval).thenReturn(Duration.ofMinutes(1))

        whenever(discovery1.isEnabled()).thenReturn(true)
        whenever(discovery2.isEnabled()).thenReturn(true)
        whenever(discovery1.discoverServers()).thenReturn(Flux.just(server1))
        whenever(discovery2.discoverServers()).thenReturn(Flux.just(server2))

        // When
        manager.afterPropertiesSet()

        // Then
        // Wait a bit for the async operations to complete
        Thread.sleep(100)

        val servers = manager.getAllServers()
        assert(servers.size == 2)
        assert(servers.any { it.id == "server1" })
        assert(servers.any { it.id == "server2" })
    }

    @Test
    fun `should not refresh servers when disabled`() {
        // Given
        whenever(discoveryProperties.enabled).thenReturn(false)

        // When
        manager.refreshServers()

        // Then
        val servers = manager.getAllServers()
        assert(servers.isEmpty())
    }

    @Test
    fun `should handle errors from discovery mechanisms`() {
        // Given
        val server1 = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 7999
        )

        whenever(discovery1.isEnabled()).thenReturn(true)
        whenever(discovery2.isEnabled()).thenReturn(true)
        whenever(discovery1.discoverServers()).thenReturn(Flux.just(server1))
        whenever(discovery2.discoverServers()).thenReturn(Flux.error(RuntimeException("Test error")))

        // When
        manager.refreshServers()

        // Then
        // Wait a bit for the async operations to complete
        Thread.sleep(100)

        val servers = manager.getAllServers()
        assert(servers.size == 1)
        assert(servers.any { it.id == "server1" })
    }

    @Test
    fun `should get server by ID`() {
        // Given
        val server1 = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 7999
        )
        val server2 = PushpinServer(
            id = "server2",
            host = "localhost",
            port = 8000
        )

        whenever(discovery1.isEnabled()).thenReturn(true)
        whenever(discovery2.isEnabled()).thenReturn(true)
        whenever(discovery1.discoverServers()).thenReturn(Flux.just(server1))
        whenever(discovery2.discoverServers()).thenReturn(Flux.just(server2))

        // When
        manager.refreshServers()

        // Then
        // Wait a bit for the async operations to complete
        Thread.sleep(100)

        val foundServer = manager.getServerById("server1")
        assert(foundServer != null)
        assert(foundServer?.id == "server1")

        val notFoundServer = manager.getServerById("nonexistent")
        assert(notFoundServer == null)
    }
}