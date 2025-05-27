# Pushpin Discovery Module

This module provides a framework for discovering Pushpin servers in various environments. It defines interfaces and implementations for different discovery mechanisms, allowing you to find and manage Pushpin servers dynamically.

## Overview

The discovery module enables your application to:
- Discover Pushpin servers from different sources (configuration, AWS, Kubernetes, etc.)
- Maintain an up-to-date list of available servers
- Refresh the server list periodically
- Retrieve servers by ID or get all available servers

## Key Components

### PushpinDiscovery Interface

The core interface that all discovery mechanisms implement:

```kotlin
interface PushpinDiscovery {
    val id: String
    fun discoverServers(): Flux<PushpinServer>
    fun isEnabled(): Boolean
}
```

### ConfigurationBasedDiscovery

A basic implementation that discovers servers from configuration properties:

```kotlin
class ConfigurationBasedDiscovery(
    private val properties: ConfigurationDiscoveryProperties,
    private val pushpinProperties: PushpinProperties,
) : PushpinDiscovery {
    // Implementation details
}
```

### PushpinDiscoveryManager

Manages multiple discovery mechanisms and maintains the list of discovered servers:

```kotlin
class PushpinDiscoveryManager(
    private val properties: DiscoveryProperties,
    private val discoveries: List<PushpinDiscovery>,
) : InitializingBean {
    // Implementation details
}
```

## Usage

### Basic Configuration

Enable discovery in your `application.properties` or `application.yml`:

```properties
# Enable discovery
pushpin.discovery.enabled=true

# Set refresh interval (how often to refresh the server list)
pushpin.discovery.refresh-interval=30s

# Enable configuration-based discovery
pushpin.discovery.configuration.enabled=true
```

### Accessing Discovered Servers

Inject the `PushpinDiscoveryManager` to access discovered servers:

```kotlin
@Service
class YourService(private val discoveryManager: PushpinDiscoveryManager) {
    
    fun doSomethingWithServers() {
        // Get all servers
        val allServers = discoveryManager.getAllServers()
        
        // Get a specific server
        val server = discoveryManager.getServerById("pushpin-1")
        
        // Use the server
        if (server != null) {
            val baseUrl = server.getBaseUrl()
            // ...
        }
    }
}
```

## Extension Points

You can create custom discovery mechanisms by implementing the `PushpinDiscovery` interface:

```kotlin
@Component
class CustomDiscovery : PushpinDiscovery {
    override val id: String = "custom"
    
    override fun discoverServers(): Flux<PushpinServer> {
        // Your custom discovery logic
        return Flux.just(
            PushpinServer(
                id = "custom-1",
                host = "custom-pushpin.example.com",
                port = 7999
                // ...
            )
        )
    }
    
    override fun isEnabled(): Boolean {
        return true
    }
}
```

## Integration with Other Modules

This module is used by:

- **pushpin-discovery-aws**: AWS-specific discovery implementation
- **pushpin-discovery-kubernetes**: Kubernetes-specific discovery implementation
- **server**: The main server application that uses discovery to find Pushpin servers

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `pushpin.discovery.enabled` | Whether discovery is enabled | `false` |
| `pushpin.discovery.refresh-interval` | How often to refresh the server list | `30s` |
| `pushpin.discovery.configuration.enabled` | Whether configuration-based discovery is enabled | `true` |

## Best Practices

1. **Enable Multiple Discovery Mechanisms**: For redundancy, enable multiple discovery mechanisms when possible
2. **Set Appropriate Refresh Intervals**: Balance between keeping the server list up-to-date and minimizing overhead
3. **Handle Discovery Failures Gracefully**: The discovery manager handles errors from individual discovery mechanisms, but your application should handle the case where no servers are available
4. **Monitor Discovery Process**: Log and monitor the discovery process to ensure it's working correctly

## Further Reading

- [AWS Discovery Module](../pushpin-discovery-aws/README.md)
- [Kubernetes Discovery Module](../pushpin-discovery-kubernetes/README.md)
