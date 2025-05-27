package io.github.mpecan.pmt.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MicrometerMetricsServiceTest {
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var metricsService: MicrometerMetricsService

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        metricsService = MicrometerMetricsService(meterRegistry)
    }

    @Test
    fun `should record messages sent`() {
        // When
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-1", "http", "success")
        metricsService.recordMessageSent("server-2", "zmq", "success")
        metricsService.recordMessageSent("server-1", "http", "failure")

        // Then
        val counter1 =
            meterRegistry
                .find(MetricNames.MESSAGES_SENT)
                .tag(MetricTags.SERVER, "server-1")
                .tag(MetricTags.TRANSPORT, "http")
                .tag(MetricTags.STATUS, "success")
                .counter()

        assertNotNull(counter1)
        assertEquals(2.0, counter1.count())

        val counter2 =
            meterRegistry
                .find(MetricNames.MESSAGES_SENT)
                .tag(MetricTags.SERVER, "server-2")
                .tag(MetricTags.TRANSPORT, "zmq")
                .tag(MetricTags.STATUS, "success")
                .counter()

        assertNotNull(counter2)
        assertEquals(1.0, counter2.count())
    }

    @Test
    fun `should record messages received`() {
        // When
        metricsService.recordMessageReceived("server-1", "websocket")
        metricsService.recordMessageReceived("server-1", "websocket")
        metricsService.recordMessageReceived("server-2", "sse")

        // Then
        val counter =
            meterRegistry
                .find(MetricNames.MESSAGES_RECEIVED)
                .tag(MetricTags.SERVER, "server-1")
                .tag(MetricTags.TRANSPORT, "websocket")
                .counter()

        assertNotNull(counter)
        assertEquals(2.0, counter.count())
    }

    @Test
    fun `should record message errors`() {
        // When
        metricsService.recordMessageError("server-1", "http", "TimeoutException")
        metricsService.recordMessageError("server-1", "http", "TimeoutException")
        metricsService.recordMessageError("server-1", "http", "IOException")

        // Then
        val timeoutCounter =
            meterRegistry
                .find(MetricNames.MESSAGES_ERRORS)
                .tag(MetricTags.SERVER, "server-1")
                .tag(MetricTags.TRANSPORT, "http")
                .tag(MetricTags.ERROR_TYPE, "TimeoutException")
                .counter()

        assertNotNull(timeoutCounter)
        assertEquals(2.0, timeoutCounter.count())

        val ioCounter =
            meterRegistry
                .find(MetricNames.MESSAGES_ERRORS)
                .tag(MetricTags.SERVER, "server-1")
                .tag(MetricTags.TRANSPORT, "http")
                .tag(MetricTags.ERROR_TYPE, "IOException")
                .counter()

        assertNotNull(ioCounter)
        assertEquals(1.0, ioCounter.count())
    }

    @Test
    fun `should record operation duration`() {
        // When
        metricsService.recordOperationDuration("publish", "server-1", 100, TimeUnit.MILLISECONDS)
        metricsService.recordOperationDuration("publish", "server-1", 200, TimeUnit.MILLISECONDS)
        metricsService.recordOperationDuration("subscribe", null, 50, TimeUnit.MILLISECONDS)

        // Then
        val publishTimer =
            meterRegistry
                .find(MetricNames.OPERATION_DURATION)
                .tag(MetricTags.OPERATION, "publish")
                .tag(MetricTags.SERVER, "server-1")
                .timer()

        assertNotNull(publishTimer)
        assertEquals(2, publishTimer.count())
        assertEquals(300.0, publishTimer.totalTime(TimeUnit.MILLISECONDS))

        val subscribeTimer =
            meterRegistry
                .find(MetricNames.OPERATION_DURATION)
                .tag(MetricTags.OPERATION, "subscribe")
                .timer()

        assertNotNull(subscribeTimer)
        assertEquals(1, subscribeTimer.count())
    }

    @Test
    fun `should record operation with timing`() {
        // When
        val result =
            metricsService.recordOperation("process", "server-1") {
                Thread.sleep(50)
                "result"
            }

        // Then
        assertEquals("result", result)

        val timer =
            meterRegistry
                .find(MetricNames.OPERATION_DURATION)
                .tag(MetricTags.OPERATION, "process")
                .tag(MetricTags.SERVER, "server-1")
                .timer()

        assertNotNull(timer)
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 50)
    }

    @Test
    fun `should update server health`() {
        // When
        metricsService.updateServerHealth("server-1", true)
        metricsService.updateServerHealth("server-2", false)

        // Then
        val healthyGauge =
            meterRegistry
                .find(MetricNames.SERVER_HEALTH)
                .tag(MetricTags.SERVER, "server-1")
                .gauge()

        assertNotNull(healthyGauge)
        assertEquals(1.0, healthyGauge.value())

        val unhealthyGauge =
            meterRegistry
                .find(MetricNames.SERVER_HEALTH)
                .tag(MetricTags.SERVER, "server-2")
                .gauge()

        assertNotNull(unhealthyGauge)
        assertEquals(0.0, unhealthyGauge.value())
    }

    @Test
    fun `should manage active connections`() {
        // When
        metricsService.incrementActiveConnections("websocket")
        metricsService.incrementActiveConnections("websocket")
        metricsService.incrementActiveConnections("sse")
        metricsService.decrementActiveConnections("websocket")

        // Then
        val wsGauge =
            meterRegistry
                .find(MetricNames.ACTIVE_CONNECTIONS)
                .tag(MetricTags.TRANSPORT, "websocket")
                .gauge()

        assertNotNull(wsGauge)
        assertEquals(1.0, wsGauge.value())

        val sseGauge =
            meterRegistry
                .find(MetricNames.ACTIVE_CONNECTIONS)
                .tag(MetricTags.TRANSPORT, "sse")
                .gauge()

        assertNotNull(sseGauge)
        assertEquals(1.0, sseGauge.value())
    }

    @Test
    fun `should update active connections directly`() {
        // When
        metricsService.updateActiveConnections("websocket", 10)
        metricsService.updateActiveConnections("sse", 5)

        // Then
        val wsGauge =
            meterRegistry
                .find(MetricNames.ACTIVE_CONNECTIONS)
                .tag(MetricTags.TRANSPORT, "websocket")
                .gauge()

        assertNotNull(wsGauge)
        assertEquals(10.0, wsGauge.value())

        val sseGauge =
            meterRegistry
                .find(MetricNames.ACTIVE_CONNECTIONS)
                .tag(MetricTags.TRANSPORT, "sse")
                .gauge()

        assertNotNull(sseGauge)
        assertEquals(5.0, sseGauge.value())
    }

    @Test
    fun `should record throughput`() {
        // When
        metricsService.recordThroughput("http", 1024)
        metricsService.recordThroughput("http", 2048)
        metricsService.recordThroughput("zmq", 512)

        // Then
        val httpCounter =
            meterRegistry
                .find(MetricNames.THROUGHPUT_BYTES)
                .tag(MetricTags.TRANSPORT, "http")
                .counter()

        assertNotNull(httpCounter)
        assertEquals(3072.0, httpCounter.count())

        val zmqCounter =
            meterRegistry
                .find(MetricNames.THROUGHPUT_BYTES)
                .tag(MetricTags.TRANSPORT, "zmq")
                .counter()

        assertNotNull(zmqCounter)
        assertEquals(512.0, zmqCounter.count())
    }

    @Test
    fun `should record publish errors`() {
        // When
        metricsService.recordPublishError("server-1", "connection_refused")
        metricsService.recordPublishError("server-1", "connection_refused")
        metricsService.recordPublishError("server-2", "timeout")

        // Then
        val counter1 =
            meterRegistry
                .find(MetricNames.PUBLISH_ERRORS)
                .tag(MetricTags.SERVER, "server-1")
                .tag(MetricTags.ERROR_TYPE, "connection_refused")
                .counter()

        assertNotNull(counter1)
        assertEquals(2.0, counter1.count())
    }

    @Test
    fun `should record connection events`() {
        // When
        metricsService.recordConnectionEvent("websocket", "opened")
        metricsService.recordConnectionEvent("websocket", "opened")
        metricsService.recordConnectionEvent("websocket", "closed")
        metricsService.recordConnectionEvent("sse", "error")

        // Then
        val openedCounter =
            meterRegistry
                .find(MetricNames.CONNECTION_EVENTS)
                .tag(MetricTags.TRANSPORT, "websocket")
                .tag(MetricTags.EVENT, "opened")
                .counter()

        assertNotNull(openedCounter)
        assertEquals(2.0, openedCounter.count())

        val closedCounter =
            meterRegistry
                .find(MetricNames.CONNECTION_EVENTS)
                .tag(MetricTags.TRANSPORT, "websocket")
                .tag(MetricTags.EVENT, "closed")
                .counter()

        assertNotNull(closedCounter)
        assertEquals(1.0, closedCounter.count())
    }

    @Test
    fun `should handle timer samples`() {
        // When
        val sample = metricsService.startTimer()
        Thread.sleep(50)
        metricsService.stopTimer(sample, "custom_operation", "server-1")

        // Then
        val timer =
            meterRegistry
                .find(MetricNames.OPERATION_DURATION)
                .tag(MetricTags.OPERATION, "custom_operation")
                .tag(MetricTags.SERVER, "server-1")
                .timer()

        assertNotNull(timer)
        assertEquals(1, timer.count())
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 50)
    }
}
