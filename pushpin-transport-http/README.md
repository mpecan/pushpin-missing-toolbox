# Pushpin HTTP Transport

This module provides HTTP transport functionality for publishing messages to Pushpin servers.

## Components

### HttpTransport
- **Purpose**: Publishes messages to Pushpin servers via HTTP
- **Features**: 
  - Publishes to multiple servers
  - Error handling and retry logic
  - Configurable timeouts
  - Reactive API using Spring WebFlux

### HttpHealthChecker  
- **Purpose**: Performs health checks on Pushpin servers via HTTP
- **Features**:
  - Non-blocking health checks
  - Configurable timeouts
  - Reactive API

### Auto-Configuration
- **HttpTransportAutoConfiguration**: Automatically configures HTTP transport components
- **Auto-discovery**: Automatically detected by Spring Boot applications

## Usage

Add the dependency to your project:

```kotlin
implementation(project(":pushpin-transport-http"))
```

The HTTP transport will be automatically configured and available for injection:

```kotlin
@Service
class MyService(private val httpTransport: HttpTransport) {
    fun publishMessage() {
        val message = Message.simple("channel", "data")
        val servers = listOf(PushpinServer("server1", "localhost", 7999))
        
        httpTransport.publishMessage(servers, message)
            .subscribe { success -> 
                println("Published: $success")
            }
    }
}
```