# Pushpin API - GRIP Protocol Implementation

This module provides a comprehensive implementation of the GRIP (Generic Realtime Intermediary Protocol) for use with Pushpin proxy servers.

## Overview

The GRIP protocol enables:
- Stateless backend development with realtime push capabilities
- HTTP long-polling, streaming, and Server-Sent Events (SSE)
- WebSocket-over-HTTP communication
- Channel-based publish-subscribe messaging
- JWT-based authentication

## Quick Start

### Add Dependency

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
}
```

### Getting Started

To start using the GRIP protocol implementation, follow these steps:

1. **Set up your Spring Boot application**:

```kotlin
@SpringBootApplication
class YourApplication

fun main(args: Array<String>) {
    runApplication<YourApplication>(*args)
}
```

2. **Create a controller for handling realtime endpoints**:

```kotlin
@RestController
@RequestMapping("/api")
class RealtimeController {

    // Long-polling endpoint
    @GetMapping("/poll/{channel}")
    fun poll(@PathVariable channel: String): ResponseEntity<Map<String, Any>> {
        // Create a response with data
        val data = mapOf("status" to "waiting for updates")

        // Add GRIP headers for long-polling
        return GripApi.longPollingResponse(channel, timeout = 30)
            .body(data)
    }

    // Server-Sent Events endpoint
    @GetMapping("/events/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(@PathVariable channel: String): ResponseEntity<String> {
        // Initial SSE message
        val initialEvent = "data: Connected to channel $channel\n\n"

        // Add GRIP headers for SSE
        return GripApi.sseResponse(channel)
            .body(initialEvent)
    }
}
```

3. **Publish messages to channels**:

```kotlin
@Service
class PublishingService {
    private val restTemplate = RestTemplate()
    private val pushpinPublishUrl = "http://localhost:5561/publish" // Adjust to your Pushpin setup

    fun publishToChannel(channel: String, data: Any) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val body = mapOf(
            "items" to listOf(
                mapOf(
                    "channel" to channel,
                    "formats" to mapOf(
                        "http-stream" to mapOf(
                            "content" to "data: ${objectMapper.writeValueAsString(data)}\n\n"
                        )
                    )
                )
            )
        )

        val request = HttpEntity(body, headers)
        restTemplate.postForEntity(pushpinPublishUrl, request, String::class.java)
    }
}
```

### Basic Usage

#### HTTP Long-Polling

```kotlin
import io.github.mpecan.pmt.grip.GripApi
import org.springframework.web.bind.annotation.*

@RestController
class LongPollingController {

    @GetMapping("/poll/{channel}")
    fun poll(@PathVariable channel: String): ResponseEntity<Map<String, Any>> {
        val response = mapOf("status" to "waiting")

        // Create response with GRIP headers for long-polling
        return GripApi.longPollingResponse(channel, timeout = 30)
            .body(response)
    }
}
```

#### HTTP Streaming

```kotlin
@GetMapping("/stream/{channel}")
fun stream(@PathVariable channel: String): ResponseEntity<Flux<String>> {
    val stream = Flux.just("Connected to $channel\n")

    // Create response with GRIP headers for streaming
    return GripApi.streamingResponse(channel)
        .body(stream)
}
```

#### Server-Sent Events (SSE)

```kotlin
@GetMapping("/events/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun events(@PathVariable channel: String): ResponseEntity<Flux<String>> {
    val events = Flux.just("data: Connected\n\n")

    // Create response with GRIP headers for SSE
    return GripApi.sseResponse(channel)
        .body(events)
}
```

#### WebSocket-over-HTTP

```kotlin
@PostMapping("/websocket/{channel}")
fun handleWebSocket(
    @PathVariable channel: String,
    @RequestHeader("Sec-WebSocket-Key") key: String,
    @RequestBody body: String
): ResponseEntity<String> {
    // Parse incoming WebSocket events
    val events = GripApi.parseWebSocketEvents(body)

    // Build response
    val response = GripApi.websocket()
        .open()
        .subscribe(channel)
        .keepAlive(timeout = 30)
        .message(mapOf("status" to "connected"))
        .build()

    // Return with WebSocket headers
    return GripApi.websocketResponse(key)
        .body(response)
}
```

## Advanced Features

### Custom GRIP Headers

Use `GripHeaderBuilder` for fine-grained control:

```kotlin
val headers = GripApi.headers()
    .holdStream()                    // Set hold mode
    .channel("notifications")        // Subscribe to channel
    .timeout(60)                     // Set timeout
    .keepAlive("ping")              // Configure keep-alive
    .previousId("msg-123")          // Set message sequencing
    .build()

// Apply to ResponseEntity
return GripApi.headers()
    .channel("updates")
    .holdResponse()
    .applyTo(ResponseEntity.ok())
    .body(data)
```

### WebSocket Message Building

Build complex WebSocket message sequences:

```kotlin
val message = GripApi.websocket()
    .open()                         // Send OPEN event
    .subscribe("chat-room")         // Subscribe to channel
    .keepAlive(timeout = 30)        // Configure keep-alive
    .text("Hello!")                 // Send text message
    .message(mapOf(                 // Send JSON message
        "type" to "notification",
        "data" to "New user joined"
    ))
    .build()
```

### GRIP Authentication

Create and validate GRIP signatures:

```kotlin
// Create a GRIP signature
val signature = GripApi.createSignature(
    issuer = "backend-service",
    key = "your-secret-key",
    expiresIn = 3600  // 1 hour
)

// Add to request headers
headers["Grip-Sig"] = signature

// Validate incoming signature
val isValid = GripApi.validateSignature(token, "your-secret-key")
```

### WebSocket Event Parsing

Parse and handle WebSocket events:

```kotlin
val events = GripApi.parseWebSocketEvents(requestBody)

events.forEach { event ->
    when (event.type) {
        WebSocketEventType.OPEN -> handleConnection()
        WebSocketEventType.TEXT -> handleTextMessage(event.content)
        WebSocketEventType.CLOSE -> handleDisconnection()
        // ... handle other event types
    }
}
```

## Control Messages

The GRIP protocol supports various control messages:

### Subscribe to Channels

```kotlin
val control = GripSubscribeControl(
    channel = "updates",
    filters = listOf("important"),
    prevId = "last-msg-id"
)
```

### Configure Keep-Alive

```kotlin
val keepAlive = GripKeepAliveControl(
    timeout = 30,
    content = "{}",
    format = "json"
)
```

### Change Hold Mode

```kotlin
val setHold = GripSetHoldControl(
    mode = "stream",
    timeout = 60,
    channels = listOf(
        GripChannelConfig("news"),
        GripChannelConfig("alerts", filters = listOf("critical"))
    )
)
```

## Complete Example

Here's a complete controller demonstrating various GRIP features:

```kotlin
@RestController
@RequestMapping("/api/grip")
class GripDemoController {

    // HTTP Long-Polling endpoint
    @GetMapping("/poll/{channel}")
    fun longPoll(@PathVariable channel: String): ResponseEntity<Mono<Message>> {
        val message = Mono.just(Message("No new messages"))

        return GripApi.longPollingResponse(channel, timeout = 20)
            .body(message)
    }

    // SSE endpoint with custom headers
    @GetMapping("/events/{channel}")
    fun serverSentEvents(
        @PathVariable channel: String,
        @RequestParam(required = false) lastEventId: String?
    ): ResponseEntity<Flux<String>> {
        val events = Flux.interval(Duration.ofSeconds(1))
            .map { "data: Event $it\n\n" }

        return GripApi.headers()
            .holdStream()
            .channel(channel)
            .previousId(lastEventId)
            .applyTo(ResponseEntity.ok())
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(events)
    }

    // WebSocket endpoint with full message handling
    @PostMapping("/ws/{channel}")
    fun websocket(
        @PathVariable channel: String,
        @RequestHeader("Sec-WebSocket-Key") key: String,
        @RequestHeader headers: HttpHeaders,
        @RequestBody body: String
    ): ResponseEntity<String> {
        val events = GripApi.parseWebSocketEvents(body)
        val messageBuilder = WebSocketMessageBuilder()

        // Handle different event types
        events.forEach { event ->
            when (event.type) {
                WebSocketEventType.OPEN -> {
                    messageBuilder
                        .open()
                        .subscribe(channel)
                        .message(mapOf("status" to "connected"))
                }
                WebSocketEventType.TEXT -> {
                    // Echo back with prefix
                    messageBuilder.text("Echo: ${event.content}")
                }
                WebSocketEventType.PING -> {
                    messageBuilder.pong()
                }
                WebSocketEventType.CLOSE -> {
                    messageBuilder.close("Goodbye")
                }
                else -> {} // Handle other types as needed
            }
        }

        // Extract and apply meta headers
        val metaHeaders = GripApi.extractMetaHeaders(headers)

        return GripApi.websocketResponse(key)
            .let { GripApi.applyMetaHeaders(it, metaHeaders) }
            .body(messageBuilder.build())
    }

    data class Message(val content: String)
}
```

## Best Practices

1. **Channel Naming**: Use hierarchical channel names (e.g., `user.123.notifications`)
2. **Message Sequencing**: Use `previousId` for ordered message delivery
3. **Keep-Alive**: Configure appropriate timeouts to prevent connection drops
4. **Error Handling**: Always handle malformed WebSocket events gracefully
5. **Authentication**: Use GRIP signatures for secure proxy-to-backend communication

## Common Patterns

### Implementing a Chat Application

A common use case for Pushpin is building a chat application. Here's how you might structure it:

1. **Channel Structure**:
   - Global channel: `chat.global`
   - Room channels: `chat.room.{roomId}`
   - Private channels: `chat.private.{userId1}.{userId2}`

2. **Backend Implementation**:
   - Create SSE endpoint for subscribing to channels
   - Create REST endpoint for sending messages
   - Use GRIP headers to manage subscriptions

3. **Client Implementation**:
   - Connect to SSE endpoint for receiving messages
   - Send POST requests to message endpoint for sending messages

### Implementing Notifications

Another common pattern is implementing a notification system:

1. **Channel Structure**:
   - User-specific channels: `notifications.user.{userId}`
   - Topic-based channels: `notifications.topic.{topicName}`

2. **Notification Types**:
   - Use the event type to differentiate between notification types
   - Include metadata for rendering different notification styles

3. **Delivery Guarantees**:
   - Use message IDs and `prevId` for ensuring ordered delivery
   - Implement client-side acknowledgment for important notifications

### Scaling Considerations

When scaling your Pushpin implementation:

1. **Multiple Pushpin Instances**:
   - Use consistent hashing for channel distribution
   - Implement a shared publishing mechanism (e.g., Redis, Kafka)

2. **High-Volume Channels**:
   - Consider channel sharding for high-volume channels
   - Implement rate limiting at the application level

3. **Monitoring**:
   - Track channel subscription counts
   - Monitor message throughput and latency
   - Set up alerts for connection drops or publishing failures

## Testing

The module includes comprehensive tests. Run them with:

```bash
./gradlew :pushpin-api:test
```

## More Information

- [GRIP Protocol Specification](https://pushpin.org/docs/protocols/grip/)
- [Pushpin Documentation](https://pushpin.org/docs/)
- [WebSocket-over-HTTP Protocol](https://pushpin.org/docs/protocols/websocket-over-http/)
