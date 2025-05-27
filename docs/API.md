# API Documentation

## Base URL

```
http://localhost:8080/api
```

## Authentication

The API supports multiple authentication methods:

### 1. Token Authentication
```http
X-Pushpin-Auth: your-secret-token
```

### 2. JWT Bearer Token
```http
Authorization: Bearer your.jwt.token
```

### 3. HMAC Signature (Server-to-Server)
```http
X-Pushpin-Signature: hmac-sha256-signature
X-Pushpin-Timestamp: 1705316400000
```

## Endpoints

### Publishing Messages

#### POST /pushpin/publish
Publish a message to a channel with full control over message properties.

**Request:**
```json
{
  "channel": "test-channel",
  "data": {
    "message": "Hello, World!",
    "timestamp": "2024-01-15T10:30:00Z"
  },
  "eventType": "notification",
  "id": "msg-123",
  "prevId": "msg-122"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Message published successfully",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

**Response (500 Error):**
```json
{
  "success": false,
  "message": "Failed to publish message",
  "error": "No healthy servers available",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

#### POST /pushpin/publish/{channel}
Simplified endpoint to publish directly to a channel.

**Request:**
```json
{
  "message": "Hello, World!",
  "user": "john.doe",
  "action": "joined"
}
```

**Query Parameters:**
- `event` (optional) - Event type for SSE clients

**Response:** Same as above

### Subscribing to Channels

#### GET /events/{channel}
Subscribe to real-time events using Server-Sent Events (SSE).

**Request:**
```http
GET /api/events/notifications
Accept: text/event-stream
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

data: {"message": "Connected to channel: notifications"}

event: user-joined
data: {"user": "john.doe", "timestamp": "2024-01-15T10:30:00Z"}

data: {"message": "System update completed"}
```

### Server Management

#### GET /pushpin/servers
List all configured Pushpin servers.

**Response:**
```json
[
  {
    "id": "pushpin-1",
    "host": "pushpin-1",
    "port": 7999,
    "controlPort": 5564,
    "publishPort": 5560,
    "active": true,
    "weight": 100,
    "healthCheckPath": "/status"
  },
  {
    "id": "pushpin-2",
    "host": "pushpin-2",
    "port": 7999,
    "controlPort": 5564,
    "publishPort": 5560,
    "active": true,
    "weight": 100,
    "healthCheckPath": "/status"
  }
]
```

#### GET /pushpin/servers/healthy
List only healthy Pushpin servers.

#### GET /pushpin/servers/{id}
Get details of a specific server.

**Response (404 Not Found):**
```json
{
  "error": "Server not found",
  "serverId": "pushpin-3"
}
```

### Monitoring & Metrics

#### GET /metrics/summary
Get a human-readable summary of system metrics.

**Response:**
```json
{
  "messages": {
    "sent": 15234,
    "received": 14980,
    "errors": 45,
    "errorRate": 0.29
  },
  "servers": {
    "pushpin-1": {
      "healthy": true,
      "host": "pushpin-1",
      "port": 7999
    },
    "pushpin-2": {
      "healthy": false,
      "host": "pushpin-2",
      "port": 7998
    }
  },
  "activeConnections": {
    "websocket": 145,
    "sse": 423,
    "http-stream": 12
  },
  "latencyMs": {
    "publish": 12.5,
    "healthCheck": 5.2
  }
}
```

#### GET /metrics/transports
Get detailed metrics by transport type.

**Response:**
```json
{
  "websocket": {
    "messagesSent": 5234,
    "messagesReceived": 5100,
    "errors": 15,
    "throughputBytes": 1048576,
    "activeConnections": 145,
    "connectionEvents": {
      "open": 1523,
      "close": 1378,
      "error": 15
    }
  },
  "sse": {
    "messagesSent": 8920,
    "messagesReceived": 8900,
    "errors": 20,
    "throughputBytes": 2097152,
    "activeConnections": 423,
    "connectionEvents": {
      "open": 2100,
      "close": 1677,
      "error": 20
    }
  }
}
```

#### GET /metrics/servers
Get performance metrics for each server.

### Health Endpoints

#### GET /actuator/health
Spring Boot health endpoint.

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 123456789012,
        "threshold": 10485760
      }
    },
    "pushpin": {
      "status": "UP",
      "details": {
        "healthyServers": 2,
        "totalServers": 2
      }
    }
  }
}
```

## Error Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error |
| 503 | Service Unavailable - No healthy servers |

## Rate Limiting

When rate limiting is enabled, the following headers are included:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705317000
```

## Message Format

### Basic Message
```json
{
  "channel": "notifications",
  "data": "Simple string message"
}
```

### Object Message
```json
{
  "channel": "notifications",
  "data": {
    "type": "alert",
    "level": "warning",
    "message": "CPU usage high"
  }
}
```

### Event Message (for SSE)
```json
{
  "channel": "notifications",
  "data": {"user": "john"},
  "eventType": "user-joined"
}
```

### Sequenced Message
```json
{
  "channel": "notifications",
  "data": {"count": 5},
  "id": "msg-125",
  "prevId": "msg-124"
}
```

## WebSocket Support

Connect to WebSocket endpoints through Pushpin:

```javascript
const ws = new WebSocket('ws://localhost:7999/websocket/chat-room');

ws.onopen = () => {
  ws.send(JSON.stringify({
    type: 'subscribe',
    channel: 'chat-room'
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received:', data);
};
```

## Examples

### Simple Notification System
```bash
# Publish a notification
curl -X POST http://localhost:8080/api/pushpin/publish/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "info",
    "title": "System Update",
    "message": "Maintenance window scheduled for tonight"
  }'
```

### Chat Message with Event Type
```bash
# Send a chat message with custom event
curl -X POST "http://localhost:8080/api/pushpin/publish/chat-room?event=message" \
  -H "Content-Type: application/json" \
  -d '{
    "user": "Alice",
    "message": "Hello everyone!",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

### Authenticated Request
```bash
# With JWT token
curl -X POST http://localhost:8080/api/pushpin/publish/secure-channel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -d '{
    "data": "Sensitive information"
  }'
```