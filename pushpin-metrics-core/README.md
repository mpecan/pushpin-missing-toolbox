# Pushpin Metrics Core

A lightweight metrics collection library for the Pushpin Missing Toolbox ecosystem. This library provides a clean metrics API that automatically uses Micrometer when available on the classpath, or falls back to a no-op implementation otherwise.

## Features

- **Automatic Implementation Selection**: Detects Micrometer on the classpath and uses it automatically
- **Zero Dependencies**: When Micrometer is not available, uses a no-op implementation with no external dependencies
- **Low-Cardinality Metrics**: Designed to avoid high-cardinality tags that can cause metrics explosion
- **Spring Boot Integration**: Includes Spring Boot autoconfiguration for seamless integration
- **Transport-Level Metrics**: Collects metrics at the transport level (HTTP, WebSocket, SSE) rather than per-channel

## Usage

### Adding the Dependency

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.mpecan.pmt:pushpin-metrics-core")
    
    // Optional: Add Micrometer for actual metrics collection
    implementation("io.micrometer:micrometer-core")
}
```

### Using the MetricsService

The library provides a `MetricsService` interface that can be injected and used:

```kotlin
@Component
class MyService(
    private val metricsService: MetricsService
) {
    fun publishMessage(server: String, transport: String) {
        try {
            // Your publishing logic here
            metricsService.recordMessageSent(server, transport, "success")
        } catch (e: Exception) {
            metricsService.recordMessageSent(server, transport, "failure")
            metricsService.recordMessageError(server, transport, e.javaClass.simpleName)
        }
    }
}
```

### Spring Boot Autoconfiguration

If you're using Spring Boot, the metrics service will be automatically configured:

```kotlin
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

The autoconfiguration will:
- Use `MicrometerMetricsService` if Micrometer is on the classpath
- Fall back to `NoOpMetricsService` if Micrometer is not available
- Allow you to override with your own `@Bean` definition if needed

### Manual Configuration

If not using Spring Boot, you can instantiate the service manually:

```kotlin
// With Micrometer
val meterRegistry = SimpleMeterRegistry()
val metricsService = MicrometerMetricsService(meterRegistry)

// Without Micrometer
val metricsService = NoOpMetricsService()
```

## Available Metrics

The library collects the following metrics:

### Counters

- `pushpin.messages.sent` - Total messages sent
  - Tags: `server`, `transport`, `status`
- `pushpin.messages.received` - Total messages received
  - Tags: `server`, `transport`
- `pushpin.messages.errors` - Total message errors
  - Tags: `server`, `transport`, `error_type`
- `pushpin.connections.established` - Total connections established
  - Tags: `server`, `transport`
- `pushpin.connections.closed` - Total connections closed
  - Tags: `server`, `transport`
- `pushpin.server.errors` - Total server errors
  - Tags: `server`, `error_type`

### Gauges

- `pushpin.active.connections` - Current number of active connections
  - Tags: `server`, `transport`
- `pushpin.active.subscriptions` - Current number of active subscriptions
  - Tags: `transport`
- `pushpin.server.active` - Number of active servers
- `pushpin.server.healthy` - Number of healthy servers

### Timers

- `pushpin.message.publish.time` - Time taken to publish messages
  - Tags: `server`, `transport`
- `pushpin.health.check.time` - Time taken for health checks
  - Tags: `server`

### Distribution Summaries

- `pushpin.throughput.bytes` - Bytes throughput
  - Tags: `transport`

## Metric Design Principles

### Low-Cardinality Tags

The library is designed to avoid high-cardinality tags that can cause metrics explosion:

- ✅ **Good**: `server` (limited number of servers), `transport` (HTTP, WebSocket, SSE)
- ❌ **Avoided**: `channel` (potentially unlimited), `user_id` (high cardinality)

### Transport-Level Aggregation

Metrics are aggregated at the transport level rather than per-channel to maintain reasonable cardinality:

```kotlin
// Instead of:
metricsService.recordMessageSent(server, transport, channel) // High cardinality!

// We use:
metricsService.recordMessageSent(server, transport, status) // Low cardinality
```

## Integration with Monitoring Systems

When using Micrometer, the metrics can be exported to various monitoring systems:

### Prometheus

```kotlin
dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

### Grafana

Example Grafana queries for dashboards:

```promql
# Message throughput by transport
rate(pushpin_messages_sent_total[5m])

# Error rate by server
rate(pushpin_messages_errors_total[5m]) / rate(pushpin_messages_sent_total[5m])

# Active connections
pushpin_active_connections

# Server health
pushpin_server_healthy / pushpin_server_active
```

## Custom Metrics Implementation

You can provide your own implementation of `MetricsService`:

```kotlin
@Component
class CustomMetricsService : MetricsService {
    override fun recordMessageSent(server: String, transport: String, status: String) {
        // Your custom implementation
    }
    
    // Implement other methods...
}
```

## Testing

The library includes comprehensive tests for all implementations:

```bash
./gradlew :pushpin-metrics-core:test
```

## License

This module is part of the Pushpin Missing Toolbox project and is licensed under the MIT License.