package io.github.mpecan.pmt.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Micrometer-based implementation of MetricsService.
 */
class MicrometerMetricsService(
    private val meterRegistry: MeterRegistry,
) : MetricsService {
    // Connection gauges by transport type
    private val activeConnections = ConcurrentHashMap<String, AtomicLong>()

    override fun recordMessageSent(server: String, transport: String, status: String) {
        Counter
            .builder(MetricNames.MESSAGES_SENT)
            .description("Total number of messages sent")
            .tag(MetricTags.SERVER, server)
            .tag(MetricTags.TRANSPORT, transport)
            .tag(MetricTags.STATUS, status)
            .register(meterRegistry)
            .increment()
    }

    override fun recordMessageReceived(server: String, transport: String) {
        Counter
            .builder(MetricNames.MESSAGES_RECEIVED)
            .description("Total number of messages received")
            .tag(MetricTags.SERVER, server)
            .tag(MetricTags.TRANSPORT, transport)
            .register(meterRegistry)
            .increment()
    }

    override fun recordMessageError(server: String, transport: String, errorType: String) {
        Counter
            .builder(MetricNames.MESSAGES_ERRORS)
            .description("Total number of message errors")
            .tag(MetricTags.SERVER, server)
            .tag(MetricTags.TRANSPORT, transport)
            .tag(MetricTags.ERROR_TYPE, errorType)
            .register(meterRegistry)
            .increment()
    }

    override fun recordOperationDuration(operation: String, server: String?, duration: Long, unit: TimeUnit) {
        val timerBuilder =
            Timer
                .builder(MetricNames.OPERATION_DURATION)
                .description("Duration of operations")
                .tag(MetricTags.OPERATION, operation)

        if (server != null) {
            timerBuilder.tag(MetricTags.SERVER, server)
        }

        timerBuilder
            .register(meterRegistry)
            .record(duration, unit)
    }

    override fun <T> recordOperation(operation: String, server: String?, block: () -> T): T {
        val timerBuilder =
            Timer
                .builder(MetricNames.OPERATION_DURATION)
                .description("Duration of operations")
                .tag(MetricTags.OPERATION, operation)

        if (server != null) {
            timerBuilder.tag(MetricTags.SERVER, server)
        }

        return timerBuilder
            .register(meterRegistry)
            .recordCallable(block)!!
    }

    override fun updateServerHealth(server: String, healthy: Boolean) {
        Gauge
            .builder(MetricNames.SERVER_HEALTH) { if (healthy) 1.0 else 0.0 }
            .description("Server health status (1=healthy, 0=unhealthy)")
            .tag(MetricTags.SERVER, server)
            .register(meterRegistry)
    }

    override fun recordServerResponseTime(server: String, endpoint: String, responseTime: Long, unit: TimeUnit) {
        Timer
            .builder(MetricNames.SERVER_RESPONSE_TIME)
            .description("Server response times")
            .tag(MetricTags.SERVER, server)
            .tag(MetricTags.ENDPOINT, endpoint)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(responseTime, unit)
    }

    override fun updateActiveConnections(transport: String, count: Long) {
        activeConnections.compute(transport) { _, current ->
            val atomic = current ?: AtomicLong(0)
            atomic.set(count)
            atomic
        }

        Gauge
            .builder(MetricNames.ACTIVE_CONNECTIONS, activeConnections) { map ->
                map[transport]?.get()?.toDouble() ?: 0.0
            }.description("Total active connections")
            .tag(MetricTags.TRANSPORT, transport)
            .register(meterRegistry)
    }

    override fun incrementActiveConnections(transport: String) {
        val current =
            activeConnections.compute(transport) { _, atomic ->
                (atomic ?: AtomicLong(0)).apply { incrementAndGet() }
            }

        Gauge
            .builder(MetricNames.ACTIVE_CONNECTIONS, current) { it.get().toDouble() }
            .description("Total active connections")
            .tag(MetricTags.TRANSPORT, transport)
            .register(meterRegistry)
    }

    override fun decrementActiveConnections(transport: String) {
        activeConnections.computeIfPresent(transport) { _, atomic ->
            atomic.apply { if (get() > 0) decrementAndGet() }
        }
    }

    override fun recordThroughput(transport: String, bytes: Long) {
        Counter
            .builder(MetricNames.THROUGHPUT_BYTES)
            .description("Total throughput in bytes")
            .baseUnit("bytes")
            .tag(MetricTags.TRANSPORT, transport)
            .register(meterRegistry)
            .increment(bytes.toDouble())
    }

    override fun recordPublishError(server: String, errorType: String) {
        Counter
            .builder(MetricNames.PUBLISH_ERRORS)
            .description("Total number of publish errors")
            .tag(MetricTags.SERVER, server)
            .tag(MetricTags.ERROR_TYPE, errorType)
            .register(meterRegistry)
            .increment()
    }

    override fun recordConnectionEvent(transport: String, event: String) {
        Counter
            .builder(MetricNames.CONNECTION_EVENTS)
            .description("Connection lifecycle events")
            .tag(MetricTags.TRANSPORT, transport)
            .tag(MetricTags.EVENT, event)
            .register(meterRegistry)
            .increment()
    }

    override fun startTimer(): MetricsService.TimerSample = MicrometerTimerSample(Timer.start(meterRegistry))

    override fun stopTimer(sample: MetricsService.TimerSample, operation: String, server: String?) {
        if (sample is MicrometerTimerSample) {
            val timerBuilder =
                Timer
                    .builder(MetricNames.OPERATION_DURATION)
                    .description("Duration of operations")
                    .tag(MetricTags.OPERATION, operation)

            if (server != null) {
                timerBuilder.tag(MetricTags.SERVER, server)
            }

            sample.micrometerSample.stop(timerBuilder.register(meterRegistry))
        }
    }

    private class MicrometerTimerSample(
        val micrometerSample: Timer.Sample,
    ) : MetricsService.TimerSample {
        override fun stop(operation: String, server: String?) {
            // Delegate to the parent stopTimer method
            throw UnsupportedOperationException("Use MetricsService.stopTimer instead")
        }
    }
}
