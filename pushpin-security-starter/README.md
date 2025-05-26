# Pushpin Security Starter

This Spring Boot starter provides a complete security solution for Pushpin-based applications. It automatically includes and configures all Pushpin security modules with sensible defaults.

## Features

The starter includes the following security modules:

- **pushpin-security-core**: Core interfaces and models
- **pushpin-security-remote**: Remote authorization client with caching
- **pushpin-security-audit**: Comprehensive audit logging
- **pushpin-security-encryption**: AES/GCM encryption for sensitive data
- **pushpin-security-hmac**: HMAC signing for server-to-server communication
- **pushpin-security-jwt**: JWT processing with multiple provider support

## Quick Start

### 1. Add Dependency

Add the starter to your Spring Boot project:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-security-starter")
}
```

### 2. Basic Configuration

Add minimal configuration to your `application.properties`:

```properties
# Enable authentication
pushpin.auth-enabled=true

# JWT Configuration (choose one provider)
pushpin.security.jwt.enabled=true
pushpin.security.jwt.provider=symmetric
pushpin.security.jwt.secret=your-secret-key-at-least-32-characters-long

# Enable audit logging
pushpin.security.audit.enabled=true
```

### 3. Run Your Application

The starter will automatically configure all security components based on your properties.

## Configuration Reference

### Global Security Settings

```properties
# Global security toggle
pushpin.security.enabled=true
```

### Remote Authorization

```properties
# Remote authorization service
pushpin.security.remote.enabled=true
pushpin.security.remote.base-url=https://auth.example.com
pushpin.security.remote.timeout=30000
pushpin.security.remote.retry-attempts=3
pushpin.security.remote.retry-delay-ms=1000

# Authorization cache
pushpin.security.remote.cache.enabled=true
pushpin.security.remote.cache.max-size=1000
pushpin.security.remote.cache.expire-after-write-minutes=5
pushpin.security.remote.cache.expire-after-access-minutes=10
```

### Audit Logging

```properties
# Audit configuration
pushpin.security.audit.enabled=true
pushpin.security.audit.include-sensitive-data=false
pushpin.security.audit.max-event-length=1000
```

### Encryption

```properties
# Data encryption
pushpin.security.encryption.enabled=true
pushpin.security.encryption.algorithm=AES/GCM/NoPadding
pushpin.security.encryption.key-size=256
pushpin.security.encryption.secret-key=your-encryption-key-base64-encoded
```

### HMAC Signing

```properties
# HMAC for server-to-server communication
pushpin.security.hmac.enabled=true
pushpin.security.hmac.secret-key=your-hmac-secret-key
pushpin.security.hmac.algorithm=HmacSHA256
pushpin.security.hmac.header-name=X-Pushpin-Signature
pushpin.security.hmac.max-age-ms=300000

# Excluded paths (optional)
pushpin.security.hmac.excluded-paths[0]=/api/public/
pushpin.security.hmac.excluded-paths[1]=/actuator/
pushpin.security.hmac.excluded-paths[2]=/api/pushpin/auth
```

### JWT Processing

```properties
# JWT authentication
pushpin.security.jwt.enabled=true

# Provider configuration (choose one)
# For development with symmetric keys:
pushpin.security.jwt.provider=symmetric
pushpin.security.jwt.secret=your-jwt-secret-at-least-32-characters

# For production with OAuth2 providers:
pushpin.security.jwt.provider=keycloak
pushpin.security.jwt.jwks-uri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/certs

# JWT validation
pushpin.security.jwt.issuer=https://your-issuer.com
pushpin.security.jwt.audience=your-audience

# Authority extraction
pushpin.security.jwt.authorities-claim=scope
pushpin.security.jwt.authorities-prefix=SCOPE_

# Channel subscription extraction
pushpin.security.jwt.claim-extraction.enabled=true
pushpin.security.jwt.claim-extraction.extract-claims[0]=$.channels
```

## Architecture Overview

The starter follows a modular architecture where each security concern is handled by a separate module:

```
pushpin-security-starter
├── pushpin-security-core (interfaces & models)
├── pushpin-security-remote (external auth)
├── pushpin-security-audit (logging)
├── pushpin-security-encryption (data protection)
├── pushpin-security-hmac (request signing)
└── pushpin-security-jwt (token processing)
```

Each module can be enabled/disabled independently via configuration properties.

## Common Configuration Patterns

### Development Setup

For local development with minimal external dependencies:

```properties
# Basic authentication
pushpin.auth-enabled=true
pushpin.security.jwt.enabled=true
pushpin.security.jwt.provider=symmetric
pushpin.security.jwt.secret=development-secret-key-32-chars

# Enable audit logging
pushpin.security.audit.enabled=true

# Disable remote authorization (use local rules)
pushpin.security.remote.enabled=false

# Disable encryption (for easier debugging)
pushpin.security.encryption.enabled=false

# Disable HMAC (for easier testing)
pushpin.security.hmac.enabled=false
```

### Production Setup

For production with external identity provider and full security:

```properties
# Production authentication
pushpin.auth-enabled=true
pushpin.security.jwt.enabled=true
pushpin.security.jwt.provider=keycloak
pushpin.security.jwt.jwks-uri=https://keycloak.company.com/auth/realms/pushpin/protocol/openid-connect/certs
pushpin.security.jwt.issuer=https://keycloak.company.com/auth/realms/pushpin
pushpin.security.jwt.audience=pushpin-api

# Remote authorization with caching
pushpin.security.remote.enabled=true
pushpin.security.remote.base-url=https://authz.company.com
pushpin.security.remote.cache.enabled=true

# Full audit logging
pushpin.security.audit.enabled=true
pushpin.security.audit.include-sensitive-data=false

# Encrypt sensitive channel data
pushpin.security.encryption.enabled=true
pushpin.security.encryption.secret-key=${ENCRYPTION_SECRET_KEY}

# HMAC for service-to-service calls
pushpin.security.hmac.enabled=true
pushpin.security.hmac.secret-key=${HMAC_SECRET_KEY}

# Channel extraction from JWT
pushpin.security.jwt.claim-extraction.enabled=true
```

### High-Security Setup

For environments requiring maximum security:

```properties
# All security features enabled
pushpin.security.enabled=true
pushpin.auth-enabled=true

# JWT with strict validation
pushpin.security.jwt.enabled=true
pushpin.security.jwt.provider=oauth2
pushpin.security.jwt.jwks-uri=https://secure-idp.company.com/.well-known/jwks.json
pushpin.security.jwt.issuer=https://secure-idp.company.com
pushpin.security.jwt.audience=pushpin-api

# Remote authorization with short cache
pushpin.security.remote.enabled=true
pushpin.security.remote.base-url=https://authz.company.com
pushpin.security.remote.cache.expire-after-write-minutes=1
pushpin.security.remote.retry-attempts=1

# Detailed audit logging
pushpin.security.audit.enabled=true
pushpin.security.audit.include-sensitive-data=true

# Strong encryption
pushpin.security.encryption.enabled=true
pushpin.security.encryption.algorithm=AES/GCM/NoPadding
pushpin.security.encryption.key-size=256

# HMAC with short validity
pushpin.security.hmac.enabled=true
pushpin.security.hmac.algorithm=HmacSHA256
pushpin.security.hmac.max-age-ms=60000
```

## Programmatic Usage

### Accessing Security Services

All security services are available as Spring beans:

```kotlin
@Service
class MyService(
    private val auditService: AuditService,
    private val encryptionService: EncryptionService,
    private val hmacService: HmacService,
    private val jwtDecoderService: JwtDecoderService,
    private val claimExtractorService: ClaimExtractorService,
    private val remoteAuthorizationClient: RemoteAuthorizationClient
) {
    
    fun processSecureData(data: String, jwt: Jwt) {
        // Audit the operation
        auditService.logDataAccess("user123", "192.168.1.1", "secure-data")
        
        // Encrypt sensitive data
        val encryptedData = encryptionService.encrypt(data)
        
        // Extract claims from JWT
        val roles = claimExtractorService.extractListClaim(jwt, "$.roles")
        
        // Check remote authorization
        val authorized = remoteAuthorizationClient.isAuthorized("user123", "channel:read")
        
        // Process...
    }
}
```

### Custom Security Configuration

Extend the default configuration if needed:

```kotlin
@Configuration
@EnableWebSecurity
class CustomSecurityConfig {
    
    @Bean
    @Primary
    fun customAuditService(): AuditService {
        return MyCustomAuditService()
    }
    
    @Bean
    fun additionalSecurityFilter(): Filter {
        return MyCustomSecurityFilter()
    }
}
```

## Migration Guide

### From Individual Modules

If you're already using individual security modules:

1. Remove individual module dependencies
2. Add the starter dependency
3. Update configuration property prefixes if needed
4. Test your application

### From Custom Security

If you have custom security implementations:

1. Add the starter dependency
2. Set `pushpin.security.enabled=false` initially
3. Enable modules one by one
4. Migrate custom logic to use the provided interfaces
5. Remove custom implementations

## Troubleshooting

### Common Issues

**Authentication not working:**
- Check `pushpin.auth-enabled=true`
- Verify JWT configuration
- Check JWT secret length (minimum 32 characters)

**Remote authorization failing:**
- Verify `pushpin.security.remote.base-url`
- Check network connectivity
- Review retry configuration
- Check audit logs for detailed errors

**HMAC verification failing:**
- Ensure same secret on client and server
- Check timestamp synchronization
- Verify header names match
- Review excluded paths configuration

**Performance issues:**
- Enable caching for remote authorization
- Adjust cache expiration times
- Review audit log verbosity
- Consider disabling features not needed

### Debug Logging

Enable debug logging for security modules:

```properties
logging.level.io.github.mpecan.pmt.security=DEBUG
```

### Health Checks

The starter automatically provides health indicators:

- JWT decoder health
- Remote authorization health  
- Encryption service health
- HMAC service health

Access via: `GET /actuator/health`

## Security Considerations

1. **Secret Management**: Never commit secrets to version control
2. **Key Rotation**: Implement regular rotation of encryption and signing keys
3. **Cache Security**: Consider cache TTL based on your security requirements
4. **Network Security**: Always use HTTPS in production
5. **Audit Retention**: Implement appropriate log retention policies
6. **Monitoring**: Set up alerts for authentication failures and security events

## Contributing

See the main project README for contribution guidelines.