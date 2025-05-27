package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.metrics.MetricNames
import io.github.mpecan.pmt.metrics.MetricTags
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/**
 * Controller for custom metrics endpoints.
 * Updated to work without high-cardinality channel tags.
 */
@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val meterRegistry: MeterRegistry,
    private val discoveryManager: PushpinDiscoveryManager,
) {
    /**
     * Returns a summary of key metrics in a human-readable format.
     */
    @GetMapping("/summary")
    fun getMetricsSummary(): Map<String, Any> {
        val summary = mutableMapOf<String, Any>()

        // Message metrics
        val messagesSent = meterRegistry.find(MetricNames.MESSAGES_SENT).counters().sumOf { it.count() }
        val messagesReceived = meterRegistry.find(MetricNames.MESSAGES_RECEIVED).counters().sumOf { it.count() }
        val messageErrors = meterRegistry.find(MetricNames.MESSAGES_ERRORS).counters().sumOf { it.count() }

        summary["messages"] =
            mapOf(
                "sent" to messagesSent,
                "received" to messagesReceived,
                "errors" to messageErrors,
                "errorRate" to if (messagesSent > 0) (messageErrors / messagesSent * 100) else 0.0,
            )

        // Server health
        val servers = discoveryManager.getAllServers()
        val serverHealth =
            servers
                .map { server ->
                    val healthGauge =
                        meterRegistry
                            .find(MetricNames.SERVER_HEALTH)
                            .tag(MetricTags.SERVER, server.id)
                            .gauge()

                    server.id to
                        mapOf(
                            "healthy" to (healthGauge?.value() == 1.0),
                            "host" to server.host,
                            "port" to server.port,
                        )
                }.toMap()

        summary["servers"] = serverHealth

        // Active connections by transport
        val activeConnections = mutableMapOf<String, Long>()
        meterRegistry
            .find(MetricNames.ACTIVE_CONNECTIONS)
            .gauges()
            .forEach { gauge ->
                val transport = gauge.id.getTag(MetricTags.TRANSPORT) ?: "unknown"
                activeConnections[transport] = gauge.value().toLong()
            }

        summary["activeConnections"] = activeConnections

        // Operation latencies (95th percentile)
        val operationLatencies = mutableMapOf<String, Double>()
        meterRegistry
            .find(MetricNames.OPERATION_DURATION)
            .timers()
            .forEach { timer ->
                val operation = timer.id.getTag(MetricTags.OPERATION) ?: "unknown"
                val p95 =
                    timer
                        .takeSnapshot()
                        .percentileValues()
                        .firstOrNull { it.percentile() == 0.95 }
                        ?.value(TimeUnit.MILLISECONDS) ?: 0.0
                operationLatencies[operation] = p95
            }

        summary["latencyMs"] = operationLatencies

        return summary
    }

    /**
     * Returns transport-level metrics.
     * Note: Channel-specific metrics are no longer available to avoid high cardinality.
     */
    @GetMapping("/transports")
    fun getTransportMetrics(): Map<String, Any> {
        val transportMetrics = mutableMapOf<String, MutableMap<String, Any>>()

        // Get unique transports from meters
        val transports = mutableSetOf<String>()
        meterRegistry.meters.forEach { meter ->
            meter.id.getTag(MetricTags.TRANSPORT)?.let { transports.add(it) }
        }

        transports.forEach { transport ->
            val metrics = mutableMapOf<String, Any>()

            // Messages sent
            val messagesSent =
                meterRegistry
                    .find(MetricNames.MESSAGES_SENT)
                    .tag(MetricTags.TRANSPORT, transport)
                    .counters()
                    .sumOf { it.count() }
            metrics["messagesSent"] = messagesSent

            // Messages received
            val messagesReceived =
                meterRegistry
                    .find(MetricNames.MESSAGES_RECEIVED)
                    .tag(MetricTags.TRANSPORT, transport)
                    .counters()
                    .sumOf { it.count() }
            metrics["messagesReceived"] = messagesReceived

            // Errors
            val errors =
                meterRegistry
                    .find(MetricNames.MESSAGES_ERRORS)
                    .tag(MetricTags.TRANSPORT, transport)
                    .counters()
                    .sumOf { it.count() }
            metrics["errors"] = errors

            // Throughput
            val throughput =
                meterRegistry
                    .find(MetricNames.THROUGHPUT_BYTES)
                    .tag(MetricTags.TRANSPORT, transport)
                    .counters()
                    .sumOf { it.count() }
            metrics["throughputBytes"] = throughput

            // Active connections
            val activeConnections =
                meterRegistry
                    .find(MetricNames.ACTIVE_CONNECTIONS)
                    .tag(MetricTags.TRANSPORT, transport)
                    .gauge()
                    ?.value() ?: 0.0
            metrics["activeConnections"] = activeConnections.toLong()

            // Connection events
            val connectionEvents = mutableMapOf<String, Long>()
            meterRegistry
                .find(MetricNames.CONNECTION_EVENTS)
                .tag(MetricTags.TRANSPORT, transport)
                .counters()
                .forEach { counter ->
                    val event = counter.id.getTag(MetricTags.EVENT) ?: "unknown"
                    connectionEvents[event] = counter.count().toLong()
                }
            metrics["connectionEvents"] = connectionEvents

            transportMetrics[transport] = metrics
        }

        return transportMetrics
    }

    /**
     * Returns server performance metrics.
     */
    @GetMapping("/servers")
    fun getServerMetrics(): Map<String, Any> {
        val serverMetrics = mutableMapOf<String, MutableMap<String, Any>>()

        discoveryManager.getAllServers().forEach { server ->
            val metrics = mutableMapOf<String, Any>()

            // Health status
            val healthGauge =
                meterRegistry
                    .find(MetricNames.SERVER_HEALTH)
                    .tag(MetricTags.SERVER, server.id)
                    .gauge()
            metrics["healthy"] = healthGauge?.value() == 1.0

            // Response times
            val responseTimer =
                meterRegistry
                    .find(MetricNames.SERVER_RESPONSE_TIME)
                    .tag(MetricTags.SERVER, server.id)
                    .timer()

            if (responseTimer != null) {
                val snapshot = responseTimer.takeSnapshot()
                metrics["responseTime"] =
                    mapOf(
                        "count" to responseTimer.count(),
                        "mean" to snapshot.mean(TimeUnit.MILLISECONDS),
                        "max" to snapshot.max(TimeUnit.MILLISECONDS),
                        "p50" to
                            snapshot.percentileValues().firstOrNull { it.percentile() == 0.5 }?.value(
                                TimeUnit.MILLISECONDS,
                            ),
                        "p95" to
                            snapshot.percentileValues().firstOrNull { it.percentile() == 0.95 }?.value(
                                TimeUnit.MILLISECONDS,
                            ),
                        "p99" to
                            snapshot.percentileValues().firstOrNull { it.percentile() == 0.99 }?.value(
                                TimeUnit.MILLISECONDS,
                            ),
                    )
            }

            // Message counts
            val messagesSent =
                meterRegistry
                    .find(MetricNames.MESSAGES_SENT)
                    .tag(MetricTags.SERVER, server.id)
                    .counters()
                    .sumOf { it.count() }

            val messageErrors =
                meterRegistry
                    .find(MetricNames.MESSAGES_ERRORS)
                    .tag(MetricTags.SERVER, server.id)
                    .counters()
                    .sumOf { it.count() }

            // Publish errors
            val publishErrors =
                meterRegistry
                    .find(MetricNames.PUBLISH_ERRORS)
                    .tag(MetricTags.SERVER, server.id)
                    .counters()
                    .sumOf { it.count() }

            metrics["messages"] =
                mapOf(
                    "sent" to messagesSent,
                    "errors" to messageErrors,
                    "publishErrors" to publishErrors,
                    "errorRate" to if (messagesSent > 0) (messageErrors / messagesSent * 100) else 0.0,
                )

            serverMetrics[server.id] = metrics
        }

        return serverMetrics
    }
}
