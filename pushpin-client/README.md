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
// Create formatter options with custom settings
val options = FormatterOptions()
    // Add custom options
    .withOption("ws.type", "binary")
    .withOption("ws.action", "send")

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

## Complete End-to-End Example

Here's a complete Spring Boot application example that demonstrates how to use the Pushpin client library:

```kotlin
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.model.Transport
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootApplication
class PushpinClientDemoApplication {
    @Bean
    fun demoRunner(pushpinService: PushpinService): CommandLineRunner = CommandLineRunner {
        // Start a background thread to publish messages every 5 seconds
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({
            try {
                pushpinService.publishMessage(
                    channel = "demo-channel",
                    message = "Hello from Pushpin client! Time: ${System.currentTimeMillis()}"
                )
                println("Message published successfully")
            } catch (e: Exception) {
                println("Error publishing message: ${e.message}")
            }
        }, 0, 5, TimeUnit.SECONDS)
    }
}

@Service
class PushpinService(private val messageSerializer: MessageSerializer) {
    private val restTemplate = RestTemplate()
    private val pushpinControlUrl = "http://localhost:5561/publish" // Pushpin Control API URL

    fun publishMessage(channel: String, message: String) {
        // Create a message for all transport types
        val pushpinMessage = Message.simple(channel, message)

        // Serialize the message
        val serializedMessage = messageSerializer.serialize(pushpinMessage)

        // Set up HTTP headers
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        // Create HTTP entity with headers and body
        val request = HttpEntity(serializedMessage, headers)

        // Send the request to Pushpin
        val response = restTemplate.postForEntity(pushpinControlUrl, request, String::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException("Failed to publish message: ${response.statusCode}")
        }
    }
}

// Run the application
fun main(args: Array<String>) {
    runApplication<PushpinClientDemoApplication>(*args)
}
```

### Client-Side Subscription Example

Here's how clients can subscribe to the channel using Server-Sent Events (SSE):

```javascript
// HTML
<div id="messages"></div>

// JavaScript
const eventSource = new EventSource('http://localhost:7999/api/events/demo-channel');

eventSource.onmessage = (event) => {
    const message = event.data;
    const messagesDiv = document.getElementById('messages');
    messagesDiv.innerHTML += `<p>${message}</p>`;
};

eventSource.onerror = (error) => {
    console.error('EventSource error:', error);
    eventSource.close();
};
```

### Error Handling Best Practices

When using the Pushpin client library, consider these error handling best practices:

1. **Wrap serialization in try-catch blocks**:
   ```kotlin
   try {
       val serializedMessage = messageSerializer.serialize(message)
       // Send the message
   } catch (e: MessageSerializationException) {
       logger.error("Failed to serialize message: ${e.message}")
       // Handle the error
   }
   ```

2. **Handle transport-specific errors**:
   ```kotlin
   try {
       val response = restTemplate.postForEntity(pushpinControlUrl, request, String::class.java)
       // Process the response
   } catch (e: RestClientException) {
       logger.error("Transport error: ${e.message}")
       // Implement retry logic or fallback
   }
   ```

3. **Use circuit breakers for resilience**:
   ```kotlin
   // With resilience4j
   @CircuitBreaker(name = "pushpin")
   fun publishWithCircuitBreaker(message: Message) {
       // Publishing logic
   }
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
