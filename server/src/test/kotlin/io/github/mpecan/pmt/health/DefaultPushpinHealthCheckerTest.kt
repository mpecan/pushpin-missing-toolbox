package io.github.mpecan.pmt.health

import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.transport.health.TransportHealthChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit

class DefaultPushpinHealthCheckerTest {
    private lateinit var httpHealthChecker: TransportHealthChecker
    private lateinit var zmqHealthChecker: TransportHealthChecker
    private lateinit var pushpinService: PushpinService
    private lateinit var metricsService: MetricsService
    private lateinit var healthChecker: DefaultPushpinHealthChecker

    private val testServer = PushpinServer("server1", "localhost", 7999)

    @BeforeEach
    fun setUp() {
        httpHealthChecker = mock()
        zmqHealthChecker = mock()
        pushpinService = mock()
        metricsService = mock()

        whenever(httpHealthChecker.getTransportType()).thenReturn("http")
        whenever(zmqHealthChecker.getTransportType()).thenReturn("zmq")
    }

    @Test
    fun `should successfully check health with default transport type`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker, zmqHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "http",
            )

        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(true)
            .verifyComplete()

        verify(metricsService).recordServerResponseTime(
            eq("server1"),
            eq("/api/health/check"),
            any(),
            eq(TimeUnit.MILLISECONDS),
        )
        verify(metricsService).updateServerHealth("server1", true)
    }

    @Test
    fun `should handle unhealthy server response`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "http",
            )

        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(false))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(false)
            .verifyComplete()

        verify(metricsService).updateServerHealth("server1", false)
    }

    @Test
    fun `should handle health check error`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "http",
            )

        val error = RuntimeException("Connection failed")
        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.error(error))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectError(RuntimeException::class.java)
            .verify()

        verify(metricsService).updateServerHealth("server1", false)
        verify(metricsService).recordMessageError("server1", "http", "RuntimeException")
    }

    @Test
    fun `should fallback to first available health checker when configured type not found`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker, zmqHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "unknown",
            )

        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(true)
            .verifyComplete()

        verify(httpHealthChecker).checkHealth(testServer)
    }

    @Test
    fun `should return false when no health checkers available`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = emptyList(),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "http",
            )

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(false)
            .verifyComplete()

        verify(metricsService).updateServerHealth("server1", false)
    }

    @Test
    fun `should use correct transport type health checker`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker, zmqHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                defaultTransportType = "zmq",
            )

        whenever(zmqHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(true)
            .verifyComplete()

        verify(zmqHealthChecker).checkHealth(testServer)
        verify(httpHealthChecker, never()).checkHealth(any())
    }

    @Test
    fun `should return all servers when health check is disabled`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = false,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        val servers = listOf(testServer, PushpinServer("server2", "localhost", 8000))
        whenever(pushpinService.getAllServers()).thenReturn(servers)

        val result = healthChecker.checkServerHealth()

        assertEquals(2, result.size)
        assertTrue(result.containsKey("server1"))
        assertTrue(result.containsKey("server2"))
        verify(httpHealthChecker, never()).checkHealth(any())
    }

    @Test
    fun `should perform health checks on all servers when enabled`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        val server2 = PushpinServer("server2", "localhost", 8000)
        val servers = listOf(testServer, server2)
        whenever(pushpinService.getAllServers()).thenReturn(servers)
        whenever(httpHealthChecker.checkHealth(any())).thenReturn(Mono.just(true))

        val result = healthChecker.checkServerHealth()

        // The result from checkServerHealth() returns the current state at call time
        // Servers are added to healthyServers asynchronously via subscribe()
        verify(httpHealthChecker).checkHealth(testServer)
        verify(httpHealthChecker).checkHealth(server2)
    }

    @Test
    fun `should return empty healthy servers initially`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        val result = healthChecker.getHealthyServers()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle mixed healthy and unhealthy servers`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        val server2 = PushpinServer("server2", "localhost", 8000)
        val servers = listOf(testServer, server2)
        whenever(pushpinService.getAllServers()).thenReturn(servers)

        // server1 healthy, server2 unhealthy
        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))
        whenever(httpHealthChecker.checkHealth(server2)).thenReturn(Mono.just(false))

        healthChecker.checkServerHealth()

        verify(metricsService).updateServerHealth("server1", true)
        verify(metricsService).updateServerHealth("server2", false)
    }

    @Test
    fun `should handle server health check errors gracefully`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        whenever(pushpinService.getAllServers()).thenReturn(listOf(testServer))
        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.error(RuntimeException("Network error")))

        healthChecker.checkServerHealth()

        verify(metricsService).updateServerHealth("server1", false)
        verify(metricsService).recordMessageError("server1", "http", "RuntimeException")
    }

    @Test
    fun `should use default transport type when not specified`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
                // defaultTransportType not specified, should default to "http"
            )

        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should record correct response times`() {
        healthChecker =
            DefaultPushpinHealthChecker(
                transportHealthCheckers = listOf(httpHealthChecker),
                healthCheckEnabled = true,
                pushpinService = pushpinService,
                metricsService = metricsService,
            )

        whenever(httpHealthChecker.checkHealth(testServer)).thenReturn(Mono.just(true))

        StepVerifier
            .create(healthChecker.checkHealth(testServer))
            .expectNext(true)
            .verifyComplete()

        verify(metricsService).recordServerResponseTime(
            eq("server1"),
            eq("/api/health/check"),
            any(), // response time will vary
            eq(TimeUnit.MILLISECONDS),
        )
    }
}
