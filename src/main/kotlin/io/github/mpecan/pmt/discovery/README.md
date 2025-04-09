# Pushpin Server Discovery

This package provides an extensible system for discovering Pushpin servers. It allows for different discovery mechanisms to be used, such as configuration-based, AWS-based, Kubernetes-based, etc.

## Architecture

The discovery system consists of the following components:

1. **PushpinDiscovery Interface**: Defines the contract for discovering Pushpin servers.
2. **Discovery Implementations**: Concrete implementations of the PushpinDiscovery interface.
3. **PushpinDiscoveryManager**: Manages the discovery process, combining results from multiple discovery mechanisms and providing a scheduling mechanism to periodically update the server list.
4. **Configuration Properties**: Configuration for the discovery system.

## Discovery Mechanisms

The following discovery mechanisms are currently implemented:

### Configuration-Based Discovery

This is the default discovery mechanism that uses the configuration properties to discover Pushpin servers. It maintains backward compatibility with the existing configuration-based approach.

Configuration:

```yaml
pushpin:
  discovery:
    configuration:
      enabled: true  # Enable/disable configuration-based discovery
  servers:
    - id: server1
      host: localhost
      port: 7999
      active: true
    - id: server2
      host: localhost
      port: 8000
      active: true
```

### AWS Discovery

This is a placeholder implementation that demonstrates how the system can be extended with AWS-based discovery. In a real implementation, this would use the AWS SDK to discover EC2 instances with specific tags and convert them to PushpinServer instances.

Configuration:

```yaml
pushpin:
  discovery:
    aws:
      enabled: false  # Enable/disable AWS-based discovery
      region: us-east-1
      tagKey: service
      tagValue: pushpin
```

### Kubernetes Discovery

This is a placeholder implementation that demonstrates how the system can be extended with Kubernetes-based discovery. In a real implementation, this would use the Kubernetes API to discover pods with specific labels and convert them to PushpinServer instances.

Configuration:

```yaml
pushpin:
  discovery:
    kubernetes:
      enabled: false  # Enable/disable Kubernetes-based discovery
      namespace: default
      labelSelector: app=pushpin
```

## Global Configuration

The discovery system can be configured globally:

```yaml
pushpin:
  discovery:
    enabled: true  # Enable/disable the discovery system
    refreshInterval: 60s  # Interval at which to refresh the server list
```

## Extending the System

To add a new discovery mechanism:

1. Create a new implementation of the PushpinDiscovery interface.
2. Add configuration properties for the new discovery mechanism.
3. Register the new discovery mechanism as a bean in the DiscoveryConfig class.

Example:

```kotlin
class MyCustomDiscovery(
    private val properties: MyCustomDiscoveryProperties
) : PushpinDiscovery {
    override val id: String = "custom"
    
    override fun discoverServers(): Flux<PushpinServer> {
        // Implement discovery logic
    }
    
    override fun isEnabled(): Boolean {
        return properties.enabled
    }
}

data class MyCustomDiscoveryProperties(
    val enabled: Boolean = false,
    // Add other properties as needed
)

// Add to DiscoveryProperties
data class DiscoveryProperties(
    // Existing properties
    val custom: MyCustomDiscoveryProperties = MyCustomDiscoveryProperties()
)

// Register in DiscoveryConfig
@Bean
fun myCustomDiscovery(
    discoveryProperties: DiscoveryProperties
): MyCustomDiscovery {
    return MyCustomDiscovery(
        discoveryProperties.custom
    )
}
```