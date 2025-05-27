# Monitoring and Metrics

The Pushpin Missing Toolbox provides comprehensive monitoring and metrics collection through a modular metrics library that integrates with Micrometer when available.

## Architecture

The metrics functionality is implemented as a separate library (`pushpin-metrics-core`) that can be used across different modules. It provides:

- **Automatic implementation selection**: Uses Micrometer when available, falls back to no-op implementation
- **Low cardinality metrics**: Designed to avoid high-cardinality tags (e.g., no channel-level metrics)
- **Transport-level aggregation**: Metrics are aggregated at the transport level (HTTP, WebSocket, SSE, ZMQ)
- **Server-level metrics**: Health status, response times, and error tracking per server

## Metrics Categories

### Message Metrics
- `pushpin.messages.sent` - Total messages sent (tags: server, transport, status)
- `pushpin.messages.received` - Total messages received (tags: server, transport)
- `pushpin.messages.errors` - Message errors (tags: server, transport, error_type)

### Connection Metrics
- `pushpin.active.connections` - Active connections by transport type
- `pushpin.connection.events` - Connection lifecycle events (opened, closed, error)

### Server Metrics
- `pushpin.server.health` - Server health status (1=healthy, 0=unhealthy)
- `pushpin.server.response.time` - Server response times with percentiles
- `pushpin.publish.errors` - Publishing errors by server

### Performance Metrics
- `pushpin.operation.duration` - Operation durations with timing
- `pushpin.throughput.bytes` - Total throughput in bytes by transport

## Configuration

### Enabling Metrics

Metrics are automatically enabled when Micrometer is on the classpath. Add the following dependencies to enable metrics collection:

```gradle
implementation("io.micrometer:micrometer-core")
implementation("io.micrometer:micrometer-registry-prometheus")
```

### Spring Boot Configuration

Add these properties to your `application.properties`:

```properties
# Enable all actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Enable Prometheus metrics
management.metrics.export.prometheus.enabled=true

# Add common tags to all metrics
management.metrics.tags.application=pushpin-missing-toolbox
management.metrics.tags.environment=production
```

## Using the Metrics Library

### Adding to Your Module

Add the dependency:

```gradle
implementation(project(":pushpin-metrics-core"))
```

### Injecting MetricsService

The `MetricsService` is automatically configured by Spring Boot:

```kotlin
@Service
class MyService(
    private val metricsService: MetricsService
) {
    fun processMessage(server: String, transport: String) {
        metricsService.recordMessageReceived(server, transport)
        
        try {
            // Process message
            metricsService.recordMessageSent(server, transport, "success")
        } catch (e: Exception) {
            metricsService.recordMessageError(server, transport, e.javaClass.simpleName)
        }
    }
}
```

### Recording Operations with Timing

```kotlin
// Manual timing
val timer = metricsService.startTimer()
// ... perform operation ...
metricsService.stopTimer(timer, "publish", server)

// Automatic timing with block
val result = metricsService.recordOperation("process", server) {
    // ... perform operation ...
    processData()
}
```

## REST API Endpoints

The server provides custom metrics endpoints:

### GET /api/metrics/summary
Returns a summary of all metrics:
```json
{
  "messages": {
    "sent": 1000,
    "received": 950,
    "errors": 10,
    "errorRate": 1.0
  },
  "servers": {
    "server-1": {
      "healthy": true,
      "host": "localhost",
      "port": 7999
    }
  },
  "activeConnections": {
    "websocket": 50,
    "http": 100
  },
  "latencyMs": {
    "publish": 15.5,
    "process": 2.3
  }
}
```

### GET /api/metrics/transports
Returns metrics grouped by transport type:
```json
{
  "http": {
    "messagesSent": 500,
    "messagesReceived": 480,
    "errors": 5,
    "throughputBytes": 1048576,
    "activeConnections": 100,
    "connectionEvents": {
      "opened": 150,
      "closed": 50
    }
  },
  "websocket": {
    "messagesSent": 300,
    "messagesReceived": 300,
    "errors": 0,
    "activeConnections": 50
  }
}
```

### GET /api/metrics/servers
Returns server-specific metrics:
```json
{
  "server-1": {
    "healthy": true,
    "responseTime": {
      "count": 1000,
      "mean": 15.5,
      "max": 250.0,
      "p50": 10.0,
      "p95": 50.0,
      "p99": 100.0
    },
    "messages": {
      "sent": 500,
      "errors": 5,
      "publishErrors": 2,
      "errorRate": 1.0
    }
  }
}
```

## Prometheus Integration

Metrics are exposed in Prometheus format at `/actuator/prometheus`:

```
# HELP pushpin_messages_sent_total Total number of messages sent
# TYPE pushpin_messages_sent_total counter
pushpin_messages_sent_total{server="server-1",transport="http",status="success"} 495.0
pushpin_messages_sent_total{server="server-1",transport="http",status="failure"} 5.0

# HELP pushpin_active_connections Total active connections
# TYPE pushpin_active_connections gauge
pushpin_active_connections{transport="websocket"} 50.0
pushpin_active_connections{transport="http"} 100.0
```

## Grafana Dashboard

A sample Grafana dashboard is provided in `monitoring/grafana/dashboards/pushpin-metrics.json`. Import this dashboard and configure it to use your Prometheus data source.

Key panels include:
- Message throughput by transport
- Error rates and types
- Active connections by transport
- Server health status
- Response time percentiles
- Operation duration heatmaps

## Alerting

Sample Prometheus alerting rules are provided in `monitoring/prometheus/alerts.yml`:

```yaml
groups:
  - name: pushpin
    rules:
      - alert: HighErrorRate
        expr: rate(pushpin_messages_errors_total[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"
          
      - alert: ServerUnhealthy
        expr: pushpin_server_health == 0
        for: 1m
        annotations:
          summary: "Pushpin server {{ $labels.server }} is unhealthy"
```

## Best Practices

1. **Avoid High Cardinality**: Don't add channel names or user IDs as metric tags
2. **Use Transport Aggregation**: Group metrics by transport type instead of individual channels
3. **Monitor Server Health**: Regularly check server health metrics
4. **Set Up Alerts**: Configure alerts for error rates and server health
5. **Use Dashboards**: Visualize metrics to identify trends and issues

## Troubleshooting

### Metrics Not Appearing

1. Check that Micrometer is on the classpath
2. Verify actuator endpoints are exposed in configuration
3. Check application logs for MetricsAutoConfiguration messages

### High Memory Usage

1. Ensure you're not creating high-cardinality metrics
2. Check for metric tag explosion (too many unique tag combinations)
3. Consider adjusting metric registry settings

### Missing Metrics

1. Verify the MetricsService is being injected (not null)
2. Check that metrics recording code is being executed
3. Look for NoOpMetricsService in logs (indicates Micrometer not found)