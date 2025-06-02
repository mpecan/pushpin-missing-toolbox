package io.github.mpecan.pmt.metrics

import java.util.concurrent.TimeUnit

/**
 * Service for managing metrics in Pushpin operations.
 * This interface provides methods for recording various metrics without high-cardinality tags.
 */
interface MetricsService {
    /**
     * Records a message sent.
     * Note: We don't include channel as a tag to avoid high cardinality.
     */
    fun recordMessageSent(
        server: String,
        transport: String,
        status: String = "success",
    )

    /**
     * Records a message received.
     */
    fun recordMessageReceived(
        server: String,
        transport: String,
    )

    /**
     * Records a message error.
     */
    fun recordMessageError(
        server: String,
        transport: String,
        errorType: String,
    )

    /**
     * Records an operation duration.
     */
    fun recordOperationDuration(
        operation: String,
        server: String?,
        duration: Long,
        unit: TimeUnit,
    )

    /**
     * Records an operation with timing.
     */
    fun <T> recordOperation(
        operation: String,
        server: String? = null,
        block: () -> T,
    ): T

    /**
     * Updates server health status.
     */
    fun updateServerHealth(
        server: String,
        healthy: Boolean,
    )

    /**
     * Records server response time.
     */
    fun recordServerResponseTime(
        server: String,
        endpoint: String,
        responseTime: Long,
        unit: TimeUnit,
    )

    /**
     * Updates total active connections count.
     * Note: We track total connections, not per-channel to avoid high cardinality.
     */
    fun updateActiveConnections(
        transport: String,
        count: Long,
    )

    /**
     * Increments active connections.
     */
    fun incrementActiveConnections(transport: String)

    /**
     * Decrements active connections.
     */
    fun decrementActiveConnections(transport: String)

    /**
     * Records total throughput in bytes.
     * Note: We don't break down by channel to avoid high cardinality.
     */
    fun recordThroughput(
        transport: String,
        bytes: Long,
    )

    /**
     * Records a publish error.
     */
    fun recordPublishError(
        server: String,
        errorType: String,
    )

    /**
     * Records connection events (opened, closed, error).
     */
    fun recordConnectionEvent(
        transport: String,
        event: String,
    )

    /**
     * Creates a timer sample for manual timing.
     */
    fun startTimer(): TimerSample

    /**
     * Stops a timer sample and records the duration.
     */
    fun stopTimer(
        sample: TimerSample,
        operation: String,
        server: String? = null,
    )

    /**
     * Timer sample interface for manual timing.
     */
    interface TimerSample {
        fun stop(
            operation: String,
            server: String? = null,
        )
    }
}

/**
 * Companion object with metric name constants.
 */
object MetricNames {
    const val MESSAGES_SENT = "pushpin.messages.sent"
    const val MESSAGES_RECEIVED = "pushpin.messages.received"
    const val MESSAGES_ERRORS = "pushpin.messages.errors"
    const val OPERATION_DURATION = "pushpin.operation.duration"
    const val SERVER_HEALTH = "pushpin.server.health"
    const val SERVER_RESPONSE_TIME = "pushpin.server.response.time"
    const val ACTIVE_CONNECTIONS = "pushpin.active.connections"
    const val THROUGHPUT_BYTES = "pushpin.throughput.bytes"
    const val PUBLISH_ERRORS = "pushpin.publish.errors"
    const val CONNECTION_EVENTS = "pushpin.connection.events"
}

/**
 * Tag name constants.
 */
object MetricTags {
    const val SERVER = "server"
    const val TRANSPORT = "transport"
    const val STATUS = "status"
    const val OPERATION = "operation"
    const val ENDPOINT = "endpoint"
    const val ERROR_TYPE = "error_type"
    const val EVENT = "event"
}
