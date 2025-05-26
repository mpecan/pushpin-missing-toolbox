package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.EncryptionService
import io.github.mpecan.pmt.service.zmq.ZmqPublisher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushpinServiceTest {

    private val webClient: WebClient = mock()
    private val messageSerializer: MessageSerializer = mock()
    private val discoveryManager: PushpinDiscoveryManager = mock()
    private val zmqPublisher: ZmqPublisher = mock()
    private val encryptionService: EncryptionService = mock()
    private val auditService: AuditService = mock()

    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var pushpinService: PushpinService

    @BeforeEach
    fun setUp() {
        // Create test properties
        val serverProps1 = PushpinProperties.ServerProperties(
            id = "test-server-1",
            host = "localhost",
            port = 7999,
            active = true
        )

        val serverProps2 = PushpinProperties.ServerProperties(
            id = "test-server-2",
            host = "localhost",
            port = 7998,
            active = true
        )

        pushpinProperties = PushpinProperties(
            servers = listOf(serverProps1, serverProps2),
            healthCheckEnabled = false
        )

        // Create security properties
        val securityProps = PushpinProperties.SecurityProperties(
            encryption = PushpinProperties.EncryptionProperties(enabled = false)
        )

        // Update properties to include security settings
        pushpinProperties = pushpinProperties.copy(
            security = securityProps
        )

        // Create service with mocked dependencies
        pushpinService = PushpinService(
            pushpinProperties,
            discoveryManager,
            messageSerializer,
            zmqPublisher,
            encryptionService,
            auditService
        )

        // Use reflection to replace the webClient with our mock
        val webClientField = PushpinService::class.java.getDeclaredField("webClient")
        webClientField.isAccessible = true
        webClientField.set(pushpinService, webClient)
    }

    @Test
    fun `getAllServers should return all configured servers`() {
        whenever(discoveryManager.getAllServers()).thenReturn(
            pushpinProperties.servers.map {
                it.toPushpinServer()
            }
        )
        val servers = pushpinService.getAllServers()

        assertEquals(2, servers.size)
        assertTrue(servers.any { it.id == "test-server-1" })
        assertTrue(servers.any { it.id == "test-server-2" })
    }

    @Test
    fun `getServerById should return the correct server`() {
        whenever(discoveryManager.getServerById("test-server-1")).thenReturn(pushpinProperties.servers[0].toPushpinServer())
        val server = pushpinService.getServerById("test-server-1")

        assertTrue(server != null)
        assertEquals("test-server-1", server.id)
        assertEquals("localhost", server.host)
        assertEquals(7999, server.port)
    }

    @Test
    fun `getServerById should return null for non-existent server`() {
        val server = pushpinService.getServerById("non-existent")

        assertTrue(server == null)
    }
}
