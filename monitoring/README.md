# Pushpin Missing Toolbox Monitoring Guide

This guide explains how to set up monitoring for the Pushpin Missing Toolbox using Prometheus and Grafana.

## Overview

The Pushpin Missing Toolbox includes comprehensive monitoring capabilities:
- **Metrics Collection**: Using Micrometer with Prometheus registry
- **Dashboards**: Pre-built Grafana dashboards for visualization
- **Alerting**: Prometheus alerting rules for critical conditions
- **Custom Endpoints**: Additional metrics endpoints for detailed insights

## Quick Start

### 1. Enable Metrics in Your Application

Metrics are enabled by default. The Prometheus endpoint is available at:
```
http://localhost:8080/actuator/prometheus
```

### 2. Configure Prometheus

Add the following job to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'pushpin-missing-toolbox'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'pushpin-missing-toolbox'
          environment: 'production'
```

### 3. Import Grafana Dashboards

1. Log into Grafana
2. Go to Dashboards → Import
3. Upload the JSON files from `monitoring/grafana/`:
   - `pushpin-overview-dashboard.json` - System overview
   - `pushpin-channels-dashboard.json` - Channel performance

### 4. Configure Alerting

Copy the alerting rules to your Prometheus configuration:
```bash
cp monitoring/prometheus/alerting-rules.yml /etc/prometheus/rules/
```

## Available Metrics

### Message Metrics
- `pushpin_messages_sent_total` - Total messages sent (counter)
- `pushpin_messages_received_total` - Total messages received (counter)
- `pushpin_messages_errors_total` - Total message errors (counter)
- `pushpin_channel_throughput_bytes_total` - Channel throughput in bytes (counter)

### Connection Metrics
- `pushpin_channel_active_connections` - Active connections per channel (gauge)
- `pushpin_websocket_connections_total` - WebSocket connection events (counter)
- `pushpin_sse_connections_total` - SSE connection events (counter)

### Performance Metrics
- `pushpin_operation_duration_seconds` - Operation duration (histogram)
- `pushpin_server_response_time_seconds` - Server response times (histogram)

### Health Metrics
- `pushpin_server_health` - Server health status (gauge: 1=healthy, 0=unhealthy)
- `pushpin_publish_errors_total` - Publish errors by type (counter)

### Standard Spring Boot Metrics
- `http_server_requests` - HTTP request metrics
- `jvm_*` - JVM metrics (memory, threads, GC)
- `process_*` - Process metrics (CPU, file descriptors)
- `system_*` - System metrics

## Custom Metrics Endpoints

### GET /api/metrics/summary
Returns a human-readable summary of key metrics:
```json
{
  "messages": {
    "sent": 1000,
    "received": 950,
    "errors": 5,
    "errorRate": 0.5
  },
  "servers": {
    "server-1": {
      "healthy": true,
      "host": "localhost",
      "port": 7999
    }
  },
  "activeConnections": {
    "channel-1:websocket": 25,
    "channel-1:sse": 10
  }
}
```

### GET /api/metrics/channels
Returns detailed metrics per channel:
```json
{
  "channel-1": {
    "messagesSent": 500,
    "messagesReceived": 480,
    "throughputBytes": 102400,
    "connections": {
      "websocket": 25,
      "sse": 10
    }
  }
}
```

### GET /api/metrics/servers
Returns performance metrics per server:
```json
{
  "server-1": {
    "healthy": true,
    "responseTime": {
      "count": 1000,
      "mean": 25.5,
      "max": 150.0,
      "p95": 45.0
    },
    "messages": {
      "sent": 1000,
      "errors": 5,
      "errorRate": 0.5
    }
  }
}
```

## Alerting Rules

The following alerts are pre-configured:

### Critical Alerts
- **PushpinHighErrorRate**: Error rate > 5% for 5 minutes
- **PushpinServerDown**: Server not responding for 2 minutes
- **PushpinMultipleServersDown**: 50% or more servers down

### Warning Alerts
- **PushpinHighLatency**: 95th percentile latency > 1s for 5 minutes
- **PushpinHighConnectionCount**: > 1000 connections per channel
- **PushpinNoMessageActivity**: No messages for 10 minutes
- **PushpinWebSocketConnectionFailures**: > 10% connection failure rate
- **PushpinServerSlowResponse**: Server response time > 500ms

## Best Practices

### 1. Metric Cardinality
- Avoid high-cardinality labels (e.g., user IDs)
- Use bounded label values
- Monitor metric count in Prometheus

### 2. Performance
- Metrics collection has minimal overhead
- Use appropriate scrape intervals (15-30s recommended)
- Consider metric sampling for high-volume operations

### 3. Retention
- Configure appropriate retention in Prometheus
- Use recording rules for frequently-queried metrics
- Archive old data if needed

### 4. Security
- Secure the metrics endpoints if exposed externally
- Use authentication for Prometheus and Grafana
- Encrypt metrics traffic with TLS

## Troubleshooting

### Metrics Not Appearing
1. Check that the application is running
2. Verify Prometheus can reach the metrics endpoint
3. Check application logs for errors
4. Ensure actuator endpoints are exposed

### High Memory Usage
1. Check metric cardinality
2. Review custom metrics for leaks
3. Adjust Prometheus retention
4. Enable metric filtering if needed

### Missing Dashboards
1. Verify Grafana data source configuration
2. Check dashboard variable settings
3. Ensure Prometheus is scraping correctly
4. Review dashboard queries for errors

## Docker Compose Example

```yaml
version: '3.8'

services:
  pushpin-toolbox:
    image: pushpin-missing-toolbox:latest
    ports:
      - "8080:8080"
    environment:
      - MANAGEMENT_METRICS_TAGS_APPLICATION=pushpin-missing-toolbox
      - MANAGEMENT_METRICS_TAGS_ENVIRONMENT=production

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/prometheus/alerting-rules.yml:/etc/prometheus/rules/alerting-rules.yml
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./monitoring/grafana:/etc/grafana/provisioning/dashboards
```

## Additional Resources

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Grafana Dashboard Guide](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)