# Pushpin Security Remote

This module provides remote authorization capabilities for Pushpin, allowing integration with external authorization services.

## Features

- Remote authorization client interface
- HTTP-based remote authorization implementation
- Caching support for authorization decisions
- Spring Boot auto-configuration

## Usage

Add the dependency to your project:

```kotlin
implementation("io.github.mpecan:pushpin-security-remote:${version}")
```

### Configuration

```yaml
pushpin:
  security:
    remote:
      enabled: true
      url: "https://your-auth-service.com"
      method: POST # or GET
      timeout: 5000
      cache:
        enabled: true
        ttl: 300 # seconds
```

### Custom Implementation

You can provide your own `RemoteAuthorizationClient` implementation:

```kotlin
@Component
class MyRemoteAuthClient : RemoteAuthorizationClient {
    override fun canSubscribe(request: HttpServletRequest, channelId: String): Boolean {
        // Your custom logic
    }
    
    override fun getSubscribableChannels(request: HttpServletRequest): Set<String> {
        // Your custom logic
    }
}
```