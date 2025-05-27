package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.metrics.MicrometerMetricsService
import io.github.mpecan.pmt.model.PushpinServer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.concurrent.TimeUnit

class MetricsControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var metricsService: MetricsService
    private lateinit var discoveryManager: PushpinDiscoveryManager
    private lateinit var metricsController: MetricsController

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        metricsService = MicrometerMetricsService(meterRegistry)
        discoveryManager = mock()
        metricsController = MetricsController(meterRegistry, discoveryManager)
        mockMvc = MockMvcBuilders.standaloneSetup(metricsController).build()

        // Setup mock servers
        val servers =
            listOf(
                PushpinServer(
                    id = "server-1",
                    host = "localhost",
                    port = 7999,
                    controlPort = 5561,
                    publishPort = 5560,
                    active = true,
                    weight = 100,
                    healthCheckPath = "/status",
                ),
                PushpinServer(
                    id = "server-2",
                    host = "localhost",
                    port = 8999,
                    controlPort = 5562,
                    publishPort = 5563,
                    active = true,
                    weight = 100,
                    healthCheckPath = "/status",
                ),
            )
        whenever(discoveryManager.getAllServers()).thenReturn(servers)
    }

    @Test
    fun `should return metrics summary`() {
        // Given - ensure metrics are recorded before test
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageError("server-1", "http", "TimeoutException")
        metricsService.recordMessageReceived("server-1", "websocket")
        metricsService.updateServerHealth("server-1", true)
        metricsService.updateServerHealth("server-2", false)
        metricsService.incrementActiveConnections("websocket")

        // Force meter registry to update
        Thread.sleep(100)

        // When & Then
        val result =
            mockMvc
                .perform(get("/api/metrics/summary"))
                .andExpect(status().isOk)
                .andReturn()

        println("Response: ${result.response.contentAsString}")

        // Check basic structure exists
        mockMvc
            .perform(get("/api/metrics/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.messages").exists())
            .andExpect(jsonPath("$.servers").exists())
            .andExpect(jsonPath("$.activeConnections").exists())
    }

    @Test
    fun `should return transport metrics`() {
        // Given
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-2", "zmq", "success")
        metricsService.recordMessageReceived("server-1", "http")
        metricsService.recordThroughput("http", 1024)
        metricsService.recordThroughput("http", 2048)
        metricsService.incrementActiveConnections("websocket")
        metricsService.incrementActiveConnections("sse")
        metricsService.recordConnectionEvent("websocket", "opened")
        metricsService.recordConnectionEvent("websocket", "closed")

        // When & Then
        mockMvc
            .perform(get("/api/metrics/transports"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.http.messagesSent").value(2.0))
            .andExpect(jsonPath("$.http.messagesReceived").value(1.0))
            .andExpect(jsonPath("$.http.throughputBytes").value(3072.0))
            .andExpect(jsonPath("$.websocket.activeConnections").value(1))
            .andExpect(jsonPath("$.websocket.connectionEvents.opened").value(1))
            .andExpect(jsonPath("$.websocket.connectionEvents.closed").value(1))
            .andExpect(jsonPath("$.zmq.messagesSent").value(1.0))
    }

    @Test
    fun `should return server metrics`() {
        // Given
        metricsService.updateServerHealth("server-1", true)
        metricsService.updateServerHealth("server-2", false)
        metricsService.recordServerResponseTime("server-1", "/status", 100, TimeUnit.MILLISECONDS)
        metricsService.recordServerResponseTime("server-1", "/status", 200, TimeUnit.MILLISECONDS)
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageError("server-1", "http", "TimeoutException")
        metricsService.recordPublishError("server-1", "connection_refused")

        // When & Then
        mockMvc
            .perform(get("/api/metrics/servers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.server-1.healthy").value(true))
            .andExpect(jsonPath("$.server-1.responseTime.count").value(2))
            .andExpect(jsonPath("$.server-1.messages.sent").value(2.0))
            .andExpect(jsonPath("$.server-1.messages.errors").value(1.0))
            .andExpect(jsonPath("$.server-1.messages.publishErrors").value(1.0))
            .andExpect(jsonPath("$.server-1.messages.errorRate").value(50.0))
            .andExpect(jsonPath("$.server-2.healthy").value(false))
    }
}
