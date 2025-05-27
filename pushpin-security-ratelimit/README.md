# Pushpin Security Rate Limit Module

This module provides rate limiting functionality for Pushpin servers to protect against abuse and ensure fair resource allocation.

## Overview

The rate limit module implements a token bucket algorithm to limit the number of requests that can be made to your Pushpin server within a specified time period. It can limit requests based on client IP address or authenticated username, providing flexibility in how you apply rate limits.

## Features

- **Token Bucket Algorithm**: Uses the Bucket4j library to implement a token bucket rate limiting algorithm
- **IP-based Rate Limiting**: Limits requests based on client IP address for unauthenticated users
- **User-based Rate Limiting**: Limits requests based on username for authenticated users
- **Configurable Limits**: Easily configure capacity and refill rates through properties
- **Spring Boot Auto-configuration**: Automatically configures when enabled
- **Audit Logging**: Logs rate limit exceeded events for monitoring and analysis

## Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-security-ratelimit:0.0.1-SNAPSHOT")
}
```

## Configuration

Enable and configure rate limiting in your `application.properties` or `application.yml` file:

```properties
# Enable rate limiting
pushpin.security.rate-limit.enabled=true

# Configure capacity (number of requests allowed)
pushpin.security.rate-limit.capacity=100

# Configure refill time in milliseconds (how often the bucket refills)
pushpin.security.rate-limit.refill-time-in-millis=60000
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `pushpin.security.rate-limit.enabled` | Whether rate limiting is enabled | `false` |
| `pushpin.security.rate-limit.capacity` | Number of requests allowed in the time period | `100` |
| `pushpin.security.rate-limit.refill-time-in-millis` | Time period for refilling tokens in milliseconds | `60000` (1 minute) |

## Usage

Once enabled and configured, the rate limiting filter will automatically apply to all incoming HTTP requests. When a client exceeds the rate limit, they will receive a `429 Too Many Requests` response with a JSON error message.

### Example Response (Rate Limit Exceeded)

```json
{
  "error": "Rate limit exceeded. Please try again later."
}
```

The response will also include an `X-Rate-Limit-Exceeded: true` header.

## Custom Configuration

You can provide your own `RateLimitFilter` bean to customize the rate limiting behavior:

```kotlin
@Configuration
class CustomRateLimitConfig {

    @Bean
    fun rateLimitFilter(
        properties: RateLimitProperties,
        auditService: AuditService
    ): RateLimitFilter {
        // Create a custom rate limit filter
        return CustomRateLimitFilter(properties, auditService)
    }
}
```

## Integration with Other Modules

This module integrates with:

- **pushpin-security-core**: Uses the `AuditService` for logging rate limit events
- **pushpin-security-starter**: Included in the security starter for a complete security solution

## Best Practices

1. **Start with Conservative Limits**: Begin with higher limits and gradually reduce them based on actual usage patterns
2. **Monitor Rate Limit Events**: Keep track of rate limit exceeded events to identify potential abuse
3. **Consider Different Limits for Different Endpoints**: For fine-grained control, consider implementing custom rate limit filters for specific endpoints
4. **Use with Authentication**: Combine with authentication for more accurate user-based rate limiting

## Further Reading

- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [Bucket4j Documentation](https://github.com/vladimir-bukhtoyarov/bucket4j)
- [Spring Boot Rate Limiting](https://www.baeldung.com/spring-bucket4j)
