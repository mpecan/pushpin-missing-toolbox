# Pushpin Security Core

Core security interfaces and models for building secure Pushpin-based applications.

## Overview

This module provides the foundational interfaces and data models for implementing security features in Pushpin applications. It's designed to be lightweight with minimal dependencies, allowing other security modules to build upon it.

## Features

- **Channel Subscription Interface**: Core `ChannelSubscriptionService` interface for determining which channels users can subscribe to
- **Audit Interface**: Core `AuditService` interface for security event logging with default no-op implementation
- **Channel Subscription Models**: Simple model classes for managing channel subscriptions (`ChannelSubscription`, `ChannelSubscriptions`)
- **Security Context**: Thread-local security context holder for maintaining security state
- **Security Exceptions**: Base exception hierarchy for security-related errors
- **Default Implementations**: NoOpAuditService ensures code using AuditService won't fail when no audit module is included

## Installation

Add the following dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.mpecan.pmt:pushpin-security-core:1.0.0")
}
```

## Usage

### Channel Subscriptions

```kotlin
// Define channel subscriptions for a user
val subscription = ChannelSubscription(
    channelId = "news-updates",
    allowed = true,
    metadata = mapOf("expiresAt" to "2024-12-31")
)

// Check if user can subscribe
val subscriptions = ChannelSubscriptions(
    principal = "user123",
    subscriptions = listOf(
        ChannelSubscription("news", allowed = true),
        ChannelSubscription("admin", allowed = false)
    ),
    defaultAllow = false
)

if (subscriptions.canSubscribe("news")) {
    // User can subscribe to this channel
}
```

### Implementing Channel Subscription Service

```kotlin
class MyChannelSubscriptionService : ChannelSubscriptionService {
    override fun canSubscribe(principal: Any?, channelId: String): Boolean {
        // Your subscription logic here
        // For example, check JWT claims, database, or external service
        return channelId.startsWith("public.") || 
               channelId == "user.$principal"
    }
    
    override fun getSubscribableChannels(principal: Any?): List<String> {
        // Return list of channels this user can subscribe to
        return listOf("public.news", "user.$principal")
    }
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