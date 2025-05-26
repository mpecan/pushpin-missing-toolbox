# Pushpin Security Audit

This module provides audit logging capabilities for Pushpin security events.

## Features

- Audit log service interface for custom implementations
- Default implementation with SLF4J logging
- Structured audit events for security-related actions
- Spring Boot auto-configuration

## Usage

Add the dependency to your project:

```kotlin
implementation("io.github.mpecan:pushpin-security-audit:${version}")
```

### Configuration

```yaml
pushpin:
  security:
    audit:
      enabled: true
      level: INFO # DEBUG, INFO, WARN, ERROR
      includeStackTrace: false
      logSuccessfulAuth: true
      logFailedAuth: true
      logChannelAccess: true
      logAdminActions: true
```

### Custom Implementation

You can provide your own `AuditLogService` implementation:

```kotlin
@Component
class MyAuditLogService : AuditLogService {
    override fun logAuthentication(userId: String?, ipAddress: String, success: Boolean, reason: String?) {
        // Your custom audit logging logic
    }
    
    override fun logChannelAccess(userId: String, ipAddress: String, channelId: String, action: String) {
        // Your custom audit logging logic
    }
    
    // Implement other methods...
}
```

### Audit Events

The module supports logging the following security events:

- **Authentication**: Successful and failed login attempts
- **Authorization**: Channel access grants and denials
- **Channel Operations**: Subscribe, unsubscribe, publish actions
- **Administrative Actions**: User management, configuration changes
- **Security Violations**: Rate limit exceeded, invalid tokens, etc.