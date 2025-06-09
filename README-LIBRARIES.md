# Pushpin Missing Toolbox - Libraries

This document provides an overview of the library components of the Pushpin Missing Toolbox project. The project follows a modular approach similar to Spring Boot, allowing you to assemble a custom solution by including only the libraries you need.

## Core Principle: Modular Assembly with Sensible Defaults

The Pushpin Missing Toolbox follows the same principle as Spring Boot: **include the libraries you need, and they will be configured with sensible defaults**. This approach allows you to:

1. **Pick and choose** only the components you need for your specific use case
2. **Auto-configure** components with reasonable defaults that work out of the box
3. **Override** specific configurations when needed
4. **Extend** functionality through a consistent API

Each library module is designed to be independent yet work seamlessly with other modules. When you include a module in your project, it will automatically configure itself with sensible defaults, but you can always customize the configuration to suit your specific needs.

## Library Modules Overview

### Core Libraries

#### pushpin-api

A comprehensive implementation of the GRIP (Generic Realtime Intermediary Protocol) for use with Pushpin proxy servers. This module provides the foundation for interacting with Pushpin using the GRIP protocol.

**Key features:**
- Support for HTTP long-polling, streaming, and Server-Sent Events (SSE)
- WebSocket-over-HTTP communication
- Channel-based publish-subscribe messaging
- JWT-based authentication

[Detailed Documentation](pushpin-api/README.md)

#### pushpin-client

A reusable Java/Kotlin library for interacting with Pushpin servers. This library provides a clean abstraction for sending messages to Pushpin in various formats.

**Key features:**
- Support for multiple transport types (WebSocket, HTTP Streaming, SSE, Long Polling)
- Support for standard Pushpin message actions (send, close, hint)
- Message IDs and sequencing
- Spring Boot auto-configuration
- Pluggable message formatting and serialization

[Detailed Documentation](pushpin-client/README.md)

#### pushpin-transport-core

Core abstractions and interfaces for transport mechanisms used to communicate with Pushpin servers.

**Key features:**
- Transport protocol abstractions
- Common transport utilities
- Base classes for implementing custom transports

#### pushpin-transport-http

HTTP-based transport implementation for communicating with Pushpin servers.

**Key features:**
- RESTful API for publishing messages
- HTTP client configuration
- Retry and circuit breaker patterns

[Detailed Documentation](pushpin-transport-http/README.md)

#### pushpin-transport-zmq

ZeroMQ-based transport implementation for high-performance communication with Pushpin servers.

**Key features:**
- High-throughput message publishing
- Low-latency communication
- Connection pooling and management

[Detailed Documentation](pushpin-transport-zmq/README.md)

### Discovery Libraries

#### pushpin-discovery

Core discovery interfaces and a configuration-based implementation for finding and managing Pushpin servers.

**Key features:**
- Service discovery abstractions
- Configuration-based server registration
- Health check integration

[Detailed Documentation](pushpin-discovery/README.md)

#### pushpin-discovery-aws

AWS-specific discovery implementation for automatically finding Pushpin servers in AWS environments.

**Key features:**
- EC2 instance discovery
- Auto Scaling Group integration
- AWS tags-based filtering

[Detailed Documentation](pushpin-discovery-aws/README.md)

#### pushpin-discovery-kubernetes

Kubernetes-specific discovery implementation for automatically finding Pushpin servers in Kubernetes clusters.

**Key features:**
- Pod and Service discovery
- Label-based filtering
- Kubernetes API integration

[Detailed Documentation](pushpin-discovery-kubernetes/README.md)

### Security Libraries

#### pushpin-security-core

Core security abstractions and interfaces for securing Pushpin communications.

**Key features:**
- Authentication and authorization interfaces
- Security context management
- Audit logging

[Detailed Documentation](pushpin-security-core/README.md)

#### pushpin-security-audit

Audit logging implementation for tracking security-related events in Pushpin communications.

**Key features:**
- Comprehensive audit logging
- Customizable audit event types
- Integration with common logging frameworks

[Detailed Documentation](pushpin-security-audit/README.md)

#### pushpin-security-encryption

Encryption utilities for securing message content in Pushpin communications.

**Key features:**
- Message payload encryption
- Key management
- Encryption algorithm selection

[Detailed Documentation](pushpin-security-encryption/README.md)

#### pushpin-security-hmac

HMAC-based authentication for securing server-to-server communications with Pushpin.

**Key features:**
- Request signing
- Signature verification
- Timestamp validation

[Detailed Documentation](pushpin-security-hmac/README.md)

#### pushpin-security-jwt

JWT-based authentication for securing client-to-server communications with Pushpin.

**Key features:**
- JWT token generation
- Token validation
- Claims management

[Detailed Documentation](pushpin-security-jwt/README.md)

#### pushpin-security-ratelimit

Rate limiting implementation to protect Pushpin servers from abuse.

**Key features:**
- Channel-based rate limiting
- Client IP-based rate limiting
- Customizable rate limit policies

[Detailed Documentation](pushpin-security-ratelimit/README.md)

#### pushpin-security-remote

Remote authentication and authorization for Pushpin servers.

**Key features:**
- Remote authentication service integration
- Authorization caching
- Fallback policies

[Detailed Documentation](pushpin-security-remote/README.md)

#### pushpin-security-starter

A Spring Boot starter that combines all security modules with sensible defaults.

**Key features:**
- Auto-configuration of security components
- Sensible security defaults
- Easy customization

[Detailed Documentation](pushpin-security-starter/README.md)

### Testing Libraries

#### pushpin-testcontainers

Testcontainers integration for testing Pushpin applications with real Pushpin instances.

**Key features:**
- Docker-based Pushpin test instances
- Support for multiple instances being started in parallel
- Automatic configuration for testing
- Integration test utilities

[Detailed Documentation](pushpin-testcontainers/README.md)

### Metrics Libraries

#### pushpin-metrics-core

Core metrics collection and reporting for Pushpin servers.

**Key features:**
- Message throughput metrics
- Connection metrics
- Health metrics
- Integration with Micrometer

[Detailed Documentation](pushpin-metrics-core/README.md)

## How to Use the Libraries

### Latest Version: 0.0.1-SNAPSHOT

### Basic Usage

To use any of the libraries, add the appropriate dependency to your project:

#### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    // Add other modules as needed
}
```

#### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT'
    implementation 'io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT'
    // Add other modules as needed
}
```

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.github.mpecan</groupId>
        <artifactId>pushpin-api</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>io.github.mpecan</groupId>
        <artifactId>pushpin-client</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    <!-- Add other modules as needed -->
</dependencies>
```

### Spring Boot Integration

Most libraries include Spring Boot auto-configuration. Simply add the dependency to your project, and the necessary beans will be registered automatically:

```kotlin
@SpringBootApplication
class YourApplication

fun main(args: Array<String>) {
    runApplication<YourApplication>(*args)
}
```

### Custom Configuration

You can customize the configuration of any module by providing your own beans or by setting properties in your `application.properties` or `application.yml` file:

```properties
# Example: Configure pushpin-client
pushpin.client.web-socket.type=binary
pushpin.client.web-socket.action=custom-action

# Example: Configure pushpin-discovery
pushpin.discovery.enabled=true
pushpin.discovery.aws.enabled=true
pushpin.discovery.aws.region=us-east-1
```

## Common Use Cases

### Basic Pushpin Integration

For basic integration with Pushpin, you typically need:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-transport-http:0.0.1-SNAPSHOT")
}
```

### Secure Pushpin Integration

For secure integration with authentication and encryption:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-transport-http:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-security-jwt:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-security-encryption:0.0.1-SNAPSHOT")
}
```

### AWS Deployment

For deployment in AWS with automatic discovery:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-transport-http:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-discovery-aws:0.0.1-SNAPSHOT")
}
```

### Kubernetes Deployment

For deployment in Kubernetes with automatic discovery:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-transport-http:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-discovery-kubernetes:0.0.1-SNAPSHOT")
}
```

### High-Performance Deployment

For high-performance deployments with ZeroMQ transport:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-api:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
    implementation("io.github.mpecan:pushpin-transport-zmq:0.0.1-SNAPSHOT")
}
```

## Best Practices

1. **Start Small**: Begin with the core modules (pushpin-api, pushpin-client) and add additional modules as needed.
2. **Use Auto-Configuration**: Let Spring Boot auto-configure the components with sensible defaults.
3. **Override Selectively**: Only override configurations that need to be customized for your specific use case.
4. **Test Thoroughly**: Use the pushpin-testcontainers module to test your integration with real Pushpin instances.
5. **Monitor Performance**: Use the pushpin-metrics-core module to collect and report metrics on your Pushpin usage.

## Further Reading

For more detailed information about each module, refer to the README.md file in the respective module's directory.

For information about the server-side of the project, see the main [README.md](README.md).
