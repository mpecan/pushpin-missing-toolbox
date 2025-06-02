package io.github.mpecan.pmt.metrics

import java.util.concurrent.TimeUnit

/**
 * No-operation implementation of MetricsService.
 * This is used when Micrometer is not available on the classpath.
 */
class NoOpMetricsService : MetricsService {
    override fun recordMessageSent(
        server: String,
        transport: String,
        status: String,
    ) {
        // No-op
    }

    override fun recordMessageReceived(
        server: String,
        transport: String,
    ) {
        // No-op
    }

    override fun recordMessageError(
        server: String,
        transport: String,
        errorType: String,
    ) {
        // No-op
    }

    override fun recordOperationDuration(
        operation: String,
        server: String?,
        duration: Long,
        unit: TimeUnit,
    ) {
        // No-op
    }

    override fun <T> recordOperation(
        operation: String,
        server: String?,
        block: () -> T,
    ): T {
        // Just execute the block without timing
        return block()
    }

    override fun updateServerHealth(
        server: String,
        healthy: Boolean,
    ) {
        // No-op
    }

    override fun recordServerResponseTime(
        server: String,
        endpoint: String,
        responseTime: Long,
        unit: TimeUnit,
    ) {
        // No-op
    }

    override fun updateActiveConnections(
        transport: String,
        count: Long,
    ) {
        // No-op
    }

    override fun incrementActiveConnections(transport: String) {
        // No-op
    }

    override fun decrementActiveConnections(transport: String) {
        // No-op
    }

    override fun recordThroughput(
        transport: String,
        bytes: Long,
    ) {
        // No-op
    }

    override fun recordPublishError(
        server: String,
        errorType: String,
    ) {
        // No-op
    }

    override fun recordConnectionEvent(
        transport: String,
        event: String,
    ) {
        // No-op
    }

    override fun startTimer(): MetricsService.TimerSample = NoOpTimerSample

    override fun stopTimer(
        sample: MetricsService.TimerSample,
        operation: String,
        server: String?,
    ) {
        // No-op
    }

    private object NoOpTimerSample : MetricsService.TimerSample {
        override fun stop(
            operation: String,
            server: String?,
        ) {
            // No-op
        }
    }
}
