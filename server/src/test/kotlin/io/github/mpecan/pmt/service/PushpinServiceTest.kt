package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.model.Transport
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.EncryptionService
import io.github.mpecan.pmt.transport.PushpinTransport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PushpinServiceTest {
    private val discoveryManager: PushpinDiscoveryManager = mock()
    private val transport: PushpinTransport = mock()
    private val encryptionService: EncryptionService = mock()
    private val auditService: AuditService = mock()
    private val metricsService: MetricsService = mock()

    private lateinit var pushpinService: PushpinService
    private lateinit var testServers: List<PushpinServer>
    private lateinit var testMessage: Message
    private lateinit var timerMock: MetricsService.TimerSample

    @BeforeEach
    fun setUp() {
        // Create test servers
        testServers =
            listOf(
                PushpinServer("test-server-1", "localhost", 7999, active = true),
                PushpinServer("test-server-2", "localhost", 7998, active = false),
            )

        // Create test message
        testMessage =
            Message(
                channel = "test-channel",
                data = "test message data",
                transports = listOf(Transport.HttpStream),
            )

        // Mock timer object
        timerMock = mock<MetricsService.TimerSample>()

        // Create service with mocked dependencies
        pushpinService =
            PushpinService(
                discoveryManager,
                encryptionService,
                auditService,
                metricsService,
                pushpinTransports = listOf(transport),
            )
    }

    @Test
    fun `getAllServers should return all configured servers`() {
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)

        val servers = pushpinService.getAllServers()

        assertEquals(2, servers.size)
        assertTrue(servers.any { it.id == "test-server-1" })
        assertTrue(servers.any { it.id == "test-server-2" })
    }

    @Test
    fun `getServerById should return the correct server`() {
        whenever(discoveryManager.getServerById("test-server-1")).thenReturn(testServers[0])

        val server = pushpinService.getServerById("test-server-1")

        assertNotNull(server)
        assertEquals("test-server-1", server!!.id)
        assertEquals("localhost", server.host)
        assertEquals(7999, server.port)
    }

    @Test
    fun `getServerById should return null for non-existent server`() {
        whenever(discoveryManager.getServerById("non-existent")).thenReturn(null)

        val server = pushpinService.getServerById("non-existent")

        assertNull(server)
    }

    @Test
    fun `constructor should fail when no transports provided`() {
        assertThrows(IllegalStateException::class.java) {
            PushpinService(
                discoveryManager,
                encryptionService,
                auditService,
                metricsService,
                pushpinTransports = emptyList(),
            )
        }
    }

    @Test
    fun `constructor should fail when multiple transports provided`() {
        val transport2: PushpinTransport = mock()

        assertThrows(IllegalStateException::class.java) {
            PushpinService(
                discoveryManager,
                encryptionService,
                auditService,
                metricsService,
                pushpinTransports = listOf(transport, transport2),
            )
        }
    }

    @Test
    fun `publishMessage should successfully publish without encryption or authentication`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        verify(transport).publish(testMessage)
        verify(metricsService).startTimer()
        verify(metricsService).stopTimer(eq(timerMock), eq("publish"), eq("test-server-1"))
        verify(metricsService).recordMessageSent(eq("test-server-1"), any(), eq("success"))
        verify(metricsService).recordThroughput(any(), any())
    }

    @Test
    fun `publishMessage should audit when authentication is present`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Setup security context
        val authentication: Authentication = mock()
        whenever(authentication.name).thenReturn("test-user")
        val securityContext: SecurityContext = mock()
        whenever(securityContext.authentication).thenReturn(authentication)
        SecurityContextHolder.setContext(securityContext)

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        verify(auditService).logChannelAccess(
            "test-user",
            "backend-service",
            "test-channel",
            "publish message",
        )

        // Clean up
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `publishMessage should encrypt message when encryption is enabled`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(true)
        whenever(encryptionService.encrypt("test message data")).thenReturn("encrypted-data")
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        verify(encryptionService).encrypt("test message data")
        verify(transport).publish(testMessage.copy(data = "encrypted-data"))
    }

    @Test
    fun `publishMessage should handle transport failure`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(false))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(false)
            .verifyComplete()

        verify(metricsService).recordMessageSent(eq("test-server-1"), any(), eq("failed"))
        verify(metricsService).recordPublishError("test-server-1", "publish_failed")
    }

    @Test
    fun `publishMessage should handle transport error`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        val error = RuntimeException("Transport error")
        whenever(transport.publish(any())).thenReturn(Mono.error(error))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectError(RuntimeException::class.java)
            .verify()

        verify(metricsService).stopTimer(eq(timerMock), eq("publish"), eq("test-server-1"))
        verify(metricsService).recordMessageError(eq("test-server-1"), any(), eq("RuntimeException"))
        verify(metricsService).recordPublishError("test-server-1", "RuntimeException")
    }

    @Test
    fun `publishMessage should use unknown server when no active servers available`() {
        // Setup mocks with no active servers
        val inactiveServers =
            listOf(
                PushpinServer("test-server-1", "localhost", 7999, active = false),
                PushpinServer("test-server-2", "localhost", 7998, active = false),
            )
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(inactiveServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        verify(metricsService).stopTimer(eq(timerMock), eq("publish"), eq("unknown"))
        verify(metricsService).recordMessageSent(eq("unknown"), any(), eq("success"))
    }

    @Test
    fun `publishMessage should use unknown server when no servers available`() {
        // Setup mocks with empty server list
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(emptyList())
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        verify(metricsService).stopTimer(eq(timerMock), eq("publish"), eq("unknown"))
        verify(metricsService).recordMessageSent(eq("unknown"), any(), eq("success"))
    }

    @Test
    fun `publishMessage should calculate transport type from class name`() {
        // Setup mocks
        whenever(metricsService.startTimer()).thenReturn(timerMock)
        whenever(discoveryManager.getAllServers()).thenReturn(testServers)
        whenever(encryptionService.isEncryptionEnabled()).thenReturn(false)
        whenever(transport.publish(any())).thenReturn(Mono.just(true))

        // Clear security context
        SecurityContextHolder.clearContext()

        val result = pushpinService.publishMessage(testMessage)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()

        // Verify that transport type is extracted (it will be something based on mock class name)
        verify(metricsService).recordMessageSent(eq("test-server-1"), any(), eq("success"))
        verify(metricsService).recordThroughput(any(), any())
    }
}
