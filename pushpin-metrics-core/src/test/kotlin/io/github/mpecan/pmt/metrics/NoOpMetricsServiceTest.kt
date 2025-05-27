package io.github.mpecan.pmt.metrics

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NoOpMetricsServiceTest {
    private lateinit var metricsService: NoOpMetricsService

    @BeforeEach
    fun setUp() {
        metricsService = NoOpMetricsService()
    }

    @Test
    fun `all methods should complete without errors`() {
        // Test that all methods can be called without throwing exceptions
        metricsService.recordMessageSent("server", "transport", "status")
        metricsService.recordMessageReceived("server", "transport")
        metricsService.recordMessageError("server", "transport", "error")
        metricsService.recordOperationDuration("operation", "server", 100, TimeUnit.MILLISECONDS)
        metricsService.updateServerHealth("server", true)
        metricsService.recordServerResponseTime("server", "endpoint", 100, TimeUnit.MILLISECONDS)
        metricsService.updateActiveConnections("transport", 10)
        metricsService.incrementActiveConnections("transport")
        metricsService.decrementActiveConnections("transport")
        metricsService.recordThroughput("transport", 1024)
        metricsService.recordPublishError("server", "error")
        metricsService.recordConnectionEvent("transport", "event")
    }

    @Test
    fun `recordOperation should execute block and return result`() {
        // When
        val result =
            metricsService.recordOperation("test", "server") {
                "test result"
            }

        // Then
        assertEquals("test result", result)
    }

    @Test
    fun `timer sample should work without errors`() {
        // When
        val sample = metricsService.startTimer()

        // Then
        assertNotNull(sample)

        // Should not throw
        metricsService.stopTimer(sample, "operation", "server")
    }

    @Test
    fun `timer sample stop method should not throw`() {
        // When
        val sample = metricsService.startTimer()

        // Then
        // This is a no-op implementation, so stop() should not throw
        // We can't directly call stop() as it's a private implementation detail
        // but we can verify it works through the service
        metricsService.stopTimer(sample, "operation", "server")
    }
}
