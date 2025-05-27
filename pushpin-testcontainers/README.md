# Pushpin Testcontainers Module

A Testcontainers implementation for [Pushpin](https://pushpin.org/) proxy server, providing easy integration testing capabilities for applications that use Pushpin.

## Overview

This module provides a configurable Testcontainer for Pushpin that allows you to:
- Spin up Pushpin instances with custom configurations
- Configure routes dynamically
- Use predefined configuration presets
- Test multi-server setups
- Integrate seamlessly with Spring Boot tests

## Installation

Add the following dependency to your project:

```kotlin
testImplementation(project(":pushpin-testcontainers"))
```

## Quick Start

### Basic Usage

```kotlin
// Create a simple Pushpin container
val pushpinContainer = PushpinContainer()
    .withHostApplicationPort(8080)
    .withSimpleHostRoute()

// Start the container
pushpinContainer.start()

// Get connection details
val httpUrl = pushpinContainer.getHttpUrl()
val publishPort = pushpinContainer.getPublishPort()
```

### Using the Builder

```kotlin
val container = PushpinContainerBuilder()
    .withHostApplicationPort(8080)
    .withRoute("api/*", "localhost:8080,over_http")
    .withRoute("ws/*", "localhost:8080,over_ws")
    .withHttpPort(7999)
    .withDebug(true)
    .build()
```

### Using Presets

```kotlin
// Use a predefined configuration
val container = PushpinContainerBuilder()
    .withPreset(PushpinPresets.webSocket())
    .withHostApplicationPort(8080)
    .build()

// Available presets:
// - minimal(): Basic configuration with debug enabled
// - webSocket(): Optimized for WebSocket connections
// - serverSentEvents(): Optimized for SSE streams
// - highThroughput(): Maximum performance settings
// - authenticated(): Includes authentication settings
// - productionLike(): HTTPS and compression enabled
// - development(): Maximum verbosity for debugging
```

## Spring Boot Integration

### Single Server Setup

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class MyIntegrationTest {
    
    companion object {
        private val SERVER_PORT = 8080
        
        @Container
        @JvmStatic
        val pushpinContainer = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
            registry.add("server.port") { SERVER_PORT }
        }
    }
}
```

### Multi-Server Setup

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class MultiServerTest {
    
    companion object {
        private val network = Network.newNetwork()
        private val SERVER_PORT = 8080
        
        @Container
        @JvmStatic
        private val pushpinContainer1 = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
            .withNetwork(network)
            .withNetworkAliases("pushpin-1")
        
        @Container
        @JvmStatic
        private val pushpinContainer2 = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
            .withNetwork(network)
            .withNetworkAliases("pushpin-2")
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configureMultiplePushpinProperties(
                registry,
                listOf(pushpinContainer1, pushpinContainer2),
                zmqEnabled = true
            )
            registry.add("server.port") { SERVER_PORT }
        }
    }
}
```

## Configuration Options

### PushpinConfiguration

The `PushpinConfiguration` data class provides all available Pushpin configuration options:

```kotlin
PushpinConfiguration(
    // Network settings
    httpPort = 7999,
    httpsPort = null,  // Optional HTTPS port
    localPort = null,  // Optional local-only port
    
    // ZMQ transport settings
    pushInSpec = "tcp://*:5560",      // PULL socket for receiving messages
    pushInSubSpec = "tcp://*:5562",   // SUB socket for subscriptions
    commandSpec = "tcp://*:5563",      // REP socket for control commands
    
    // HTTP transport settings
    pushInHttpPort = 5561,  // HTTP endpoint for message publishing
    
    // Routing
    routesFile = "/etc/pushpin/routes",
    
    // Performance
    maxWorkers = null,          // Max worker processes
    clientBufferSize = 200000,  // Client buffer size
    maxChannelIdLength = 64,    // Max channel ID length
    messageRate = 2500,         // Messages per second limit
    
    // Debugging
    debug = false,
    logLevel = 1,              // 1-4 (1=error, 2=warning, 3=info, 4=debug)
    quietMode = false,
    
    // Security
    ipcFileMode = 777,
    allowOrigins = "*",
    allowHeaders = null,
    
    // Advanced options...
)
```

### PushpinContainerBuilder Methods

- `withHostApplicationPort(port: Int)`: Set the port your application runs on
- `withHttpPort(port: Int)`: Set Pushpin's HTTP proxy port
- `withPublishPort(port: Int)`: Set the ZMQ PULL socket port
- `withControlPort(port: Int)`: Set the ZMQ REP control port
- `withRoute(pattern: String, target: String)`: Add a routing rule
- `withRoutes(routes: Map<String, String>)`: Set multiple routes
- `withSimpleHostRoute(pattern: String = "*")`: Route all traffic to host app
- `withDebug(enabled: Boolean)`: Enable/disable debug mode
- `withLogLevel(level: Int)`: Set log verbosity (1-4)
- `withConfiguration(config: PushpinConfiguration)`: Use custom configuration
- `withConfiguration(configure: PushpinConfiguration.() -> PushpinConfiguration)`: Configure with lambda
- `withPreset(preset: PushpinConfiguration)`: Apply a configuration preset
- `withDockerImage(image: String)`: Use custom Docker image

## Container Methods

Once started, the container provides these methods:

- `getHttpPort()`: Get the mapped HTTP proxy port
- `getPublishPort()`: Get the mapped ZMQ publish port
- `getHttpPublishPort()`: Get the mapped HTTP publish port
- `getControlPort()`: Get the mapped control port
- `getSubPort()`: Get the mapped SUB port
- `getHttpUrl()`: Get the full HTTP URL
- `getPublishUrl()`: Get the ZMQ publish URL
- `getControlUrl()`: Get the ZMQ control URL
- `getHttpPublishUrl()`: Get the HTTP publish URL

## Routing Configuration

Routes define how Pushpin forwards requests to your backend:

```kotlin
// Simple route to host application
.withSimpleHostRoute()  // Routes * to host.testcontainers.internal:port

// Custom routes
.withRoute("api/*", "backend:8080,over_http")
.withRoute("ws/*", "backend:8080,over_ws")
.withRoute("sse/*", "backend:8080,over_sse")

// Multiple routes at once
.withRoutes(mapOf(
    "api/*" to "api-server:8080,over_http",
    "auth/*" to "auth-server:9000,over_http",
    "ws/*" to "ws-server:8081,over_ws"
))
```

## TestcontainersUtils

The `TestcontainersUtils` object provides helper methods for Spring Boot integration:

```kotlin
// Create a configured container
val container = TestcontainersUtils.createPushpinContainer(8080)

// Configure Spring properties for single container
TestcontainersUtils.configurePushpinProperties(registry, container)

// Configure Spring properties for multiple containers
TestcontainersUtils.configureMultiplePushpinProperties(
    registry, 
    containers,
    zmqEnabled = true
)
```

## Troubleshooting

### Container Won't Start
- Check that the required ports are not already in use
- Verify Docker is running and accessible
- Check container logs with `container.logs`

### Routes Not Working
- Ensure `withAccessToHost(true)` is enabled (done by default)
- Verify the host port is exposed with `Testcontainers.exposeHostPorts(port)`
- Check the routes configuration in container logs

### Connection Refused
- Make sure to use the mapped ports, not the container ports
- Use `localhost` or `container.host` for connections
- Wait for the container to be fully started

## Examples

See the test files in this module for more examples:
- `PushpinContainerTest.kt`: Basic container usage
- `PushpinContainerBuilderTest.kt`: Builder pattern examples
- `PushpinPresetsTest.kt`: Using configuration presets

## License

This module is part of the Pushpin Missing Toolbox project and follows the same license terms.