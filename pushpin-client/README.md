# Pushpin Client Library

A reusable Java/Kotlin library for interacting with Pushpin servers. This library provides a clean abstraction for sending messages to Pushpin in various formats, supporting different transport mechanisms like WebSocket, HTTP Streaming, Server-Sent Events, and long polling.

## What is Pushpin?

[Pushpin](https://pushpin.org/) is a reverse proxy server that makes it easy to implement realtime API endpoints. It proxies HTTP and WebSocket traffic to your backend services, and handles the connection management for realtime features.

This client library simplifies the process of publishing messages to Pushpin, which then delivers those messages to connected clients.

## Features

- Support for multiple transport types (as defined in the GRIP protocol):
  - WebSocket
  - HTTP Streaming
  - Server-Sent Events (SSE)
  - HTTP Response
  - Long Polling
- Support for standard Pushpin message actions:
  - `send` - Deliver content to subscribers
  - `close` - End associated requests/connections
  - `hint` - Clients must retrieve content from another source
- Support for message IDs and sequencing
- Clean, intuitive API for message creation and serialization
- Spring Boot auto-configuration
- Pluggable message formatting and serialization
- Comprehensive testing

## Installation

### Maven

```xml
<dependency>
  <groupId>io.github.mpecan</groupId>
  <artifactId>pushpin-client</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Gradle

```kotlin
implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
```

## Usage

### With Spring Boot

The library includes Spring Boot auto-configuration. Simply add the dependency to your project, and the necessary beans will be registered automatically.

```kotlin
@Service
class YourService(
    private val messageSerializer: MessageSerializer
) {
    fun publishMessage(channel: String, data: Any) {
        // Create a message
        val message = Message.simple(channel, data)
        
        // Serialize the message to Pushpin format
        val pushpinMessage = messageSerializer.serialize(message)
        
        // Send the message to your Pushpin server
        // ...
    }
}
```

### Without Spring Boot

You can also use the library without Spring Boot:

```kotlin
// Create serialization service
val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
}
val serializationService = JacksonMessageSerializationService(objectMapper)

// Create formatters
val webSocketFormatter = DefaultWebSocketMessageFormatter(serializationService)
val sseFormatter = HttpSSEStreamMessageFormatter(serializationService)
val httpStreamFormatter = SimpleHttpStreamMessageFormatter(serializationService)
val httpResponseFormatter = DefaultHttpResponseMessageFormatter(serializationService)
val longPollingFormatter = DefaultLongPollingMessageFormatter(serializationService)

// Create serializer
val messageSerializer = DefaultMessageSerializer(
    webSocketFormatter,
    sseFormatter,
    httpStreamFormatter,
    httpResponseFormatter,
    longPollingFormatter
)

// Create and serialize a message
val message = Message(
    channel = "my-channel",
    data = mapOf("message" to "Hello, World!"),
    transports = listOf(Transport.WebSocket, Transport.HttpStreamSSE)
)
val pushpinMessage = messageSerializer.serialize(message)

// Send the message to your Pushpin server
// ...
```

## Message Creation

The library provides several ways to create messages:

```kotlin
// Simple message with channel and data
val message1 = Message.simple("my-channel", "Hello, World!")

// Event message with channel, event type, and data
val message2 = Message.event("my-channel", "user-joined", mapOf("user" to "john"))

// Message with metadata
val message3 = Message.withMeta(
    "my-channel", 
    "Hello, World!", 
    mapOf("priority" to "high")
)

// Message with tracking IDs (for message ordering and deduplication)
val message4 = Message.withIds(
    "my-channel",
    "Hello, World!",
    id = "msg-123",  // Current message ID
    prevId = "msg-122"  // Previous message ID in sequence
)

// Transport-specific messages
val wsOnly = Message.webSocketOnly("my-channel", "Hello, WebSocket clients!")
val sseOnly = Message.sseOnly("my-channel", "Hello, SSE clients!")
val httpStreamOnly = Message.httpStreamOnly("my-channel", "Hello, HTTP stream clients!")

// Custom message with all properties
val message5 = Message.custom(
    channel = "my-channel",
    data = "Hello, World!",
    eventType = "greeting",
    meta = mapOf("importance" to "high"),
    id = "msg-123",
    prevId = "msg-122",
    transports = listOf(Transport.WebSocket, Transport.HttpStreamSSE)
)
```

### Message Instance Methods

You can also use instance methods to modify existing messages:

```kotlin
// Start with a simple message
val message = Message.simple("my-channel", "Hello, World!")

// Add metadata
val withMeta = message.addMeta(mapOf("priority" to "high"))

// Change event type
val withEvent = message.withEventType("greeting")

// Change transports
val withDifferentTransports = message.withTransports(listOf(Transport.WebSocket))

// Add tracking IDs
val withIds = message.withIds(id = "msg-123", prevId = "msg-122")
```

## Pushpin Actions

Pushpin supports different actions for each transport format:

### WebSocket Actions

```kotlin
// Create a WebSocket formatter for sending messages (default)
val sendFormatter = formatterFactory.createWebSocketFormatter()

// Create a WebSocket formatter for closing connections
val closeFormatter = formatterFactory.createWebSocketFormatter(
    FormatterOptions().withOption("ws.action", WebSocketFormat.ACTION_CLOSE)
        .withOption("ws.close.code", 1000)
)

// Or use the convenience method
val closeFormatter = webSocketFormatter.withCloseAction(1000)

// Create a WebSocket formatter for hint action
val hintFormatter = formatterFactory.createWebSocketFormatter(
    FormatterOptions().withOption("ws.action", WebSocketFormat.ACTION_HINT)
)
```

### HTTP Stream Actions

```kotlin
// Send content to HTTP stream clients
val httpStreamFormat = HttpStreamFormat.send("Hello, stream clients!")

// Close HTTP stream connections
val closeFormat = HttpStreamFormat.close()
```

## Creating Custom Message Serializers

You can create custom message serializers using the `MessageSerializerBuilder`:

```kotlin
// Create a serializer with the default formatters from a factory
val defaultSerializer = MessageSerializerBuilder.defaultSerializer(formatterFactory)

// Create a serializer with custom formatters
val customSerializer = MessageSerializerBuilder.builder()
    .withFormatterFactory(formatterFactory)  // Optional, for any unspecified formatters
    .withWebSocketFormatter(customWebSocketFormatter)
    .withHttpSseStreamFormatter(customSSEFormatter)
    .withHttpStreamFormatter(customHttpStreamFormatter)
    .withHttpResponseFormatter(customHttpResponseFormatter)
    .withLongPollingFormatter(customLongPollingFormatter)
    .build()
```

## Customizing Message Formatting

### Using FormatterOptions

The library provides a flexible way to customize message formatting using `FormatterOptions`:

```kotlin
// Create formatter options with custom pre-processor and post-processor
val options = FormatterOptions()
    .withPreProcessor { message ->
        // Pre-process the message before formatting
        message.addMeta(mapOf("timestamp" to System.currentTimeMillis()))
    }
    .withPostProcessor { format, message ->
        // Post-process the format after formatting
        // This is type-specific, so you need to cast
        if (format is WebSocketFormat) {
            format.copy(action = "custom-action")
        } else {
            format
        }
    }
    .withOption("ws.type", "binary")

// Create a formatter with these options
val formatterFactory = DefaultFormatterFactory(serializationService)
val webSocketFormatter = formatterFactory.createWebSocketFormatter(options)
```

### Using Spring Boot Configuration

When using Spring Boot, you can configure formatters using properties:

```properties
# WebSocket formatter configuration
pushpin.client.web-socket.type=binary
pushpin.client.web-socket.action=custom-action

# HTTP Response formatter configuration
pushpin.client.http-response.code=200
pushpin.client.http-response.reason=OK
pushpin.client.http-response.headers.Content-Type=application/json

# SSE formatter configuration
pushpin.client.sse.event-name=custom-event
```

### Custom Formatter Implementations

You can also provide completely custom implementations of message formatters:

```kotlin
class CustomWebSocketMessageFormatter(
    serializationService: MessageSerializationService,
    options: FormatterOptions = FormatterOptions()
) : AbstractMessageFormatter<WebSocketFormat>(serializationService, options), WebSocketMessageFormatter {
    
    override fun doFormat(message: Message): WebSocketFormat {
        // Custom formatting logic
        return WebSocketFormat(
            content = serializationService.serialize(message),
            type = "text",
            action = if (message.meta?.get("action") != null) {
                message.meta["action"] as String
            } else {
                "send"
            }
        )
    }
}
```

### Registering Custom Formatters with Spring Boot

```kotlin
@Configuration
class CustomFormattersConfig {
    
    @Bean
    @Primary
    fun customWebSocketFormatter(serializationService: MessageSerializationService): WebSocketMessageFormatter {
        return CustomWebSocketMessageFormatter(serializationService)
    }
}
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.