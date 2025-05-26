# Pushpin Security Core

Core security interfaces and models for building secure Pushpin-based applications.

## Overview

This module provides the foundational interfaces and data models for implementing security features in Pushpin applications. It's designed to be lightweight with minimal dependencies, allowing other security modules to build upon it.

## Features

- **Authorization Interface**: Core `AuthorizationService` interface for implementing custom authorization strategies
- **Audit Interface**: Core `AuditService` interface for security event logging
- **Channel Permissions**: Model classes for managing channel-based permissions (`ChannelPermission`, `ChannelPermissions`)
- **Security Context**: Thread-local security context holder for maintaining security state
- **Security Exceptions**: Base exception hierarchy for security-related errors

## Installation

Add the following dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.mpecan.pmt:pushpin-security-core:1.0.0")
}
```

## Usage

### Channel Permissions

```kotlin
// Define permissions for a channel
val permissions = ChannelPermissions(
    channelId = "news-updates",
    permissions = setOf(ChannelPermission.READ, ChannelPermission.WRITE)
)

// Check permissions
if (permissions.hasWritePermission()) {
    // User can publish to this channel
}
```

### Implementing Authorization

```kotlin
class MyAuthorizationService : AuthorizationService {
    override fun hasPermission(
        principal: Any?, 
        channelId: String, 
        permission: ChannelPermission
    ): Boolean {
        // Your authorization logic here
        return true
    }
    
    // Implement other methods...
}
```

### Security Context

```kotlin
// Set security context
val context = SecurityContext(
    principal = "user123",
    authenticated = true,
    attributes = mapOf("role" to "ADMIN")
)
SecurityContextHolder.setContext(context)

// Retrieve security context
val currentContext = SecurityContextHolder.getContext()
val role = currentContext.getAttribute<String>("role")
```

## License

MIT License