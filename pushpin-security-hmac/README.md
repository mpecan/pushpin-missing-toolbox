# Pushpin Security HMAC

This module provides HMAC (Hash-based Message Authentication Code) functionality for secure server-to-server communication in the Pushpin ecosystem.

## Features

- HMAC signature generation and verification
- Request signature validation with replay attack prevention
- Configurable algorithms (default: HmacSHA256)
- Spring Boot AutoConfiguration
- Audit logging integration

## Usage

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-security-hmac")
}
```

## Configuration

Configure HMAC in your `application.properties` or `application.yml`:

```properties
# Enable HMAC signing
pushpin.security.hmac.enabled=true

# Secret key for HMAC signing (required when enabled)
pushpin.security.hmac.secret-key=your-secret-key

# HMAC algorithm (optional, default: HmacSHA256)
pushpin.security.hmac.algorithm=HmacSHA256

# Header name for signature (optional, default: X-Pushpin-Signature)
pushpin.security.hmac.header-name=X-Pushpin-Signature

# Maximum age of requests in milliseconds (optional, default: 300000 = 5 minutes)
pushpin.security.hmac.max-age-ms=300000

# Excluded paths from HMAC verification (optional)
pushpin.security.hmac.excluded-paths[0]=/api/public/
pushpin.security.hmac.excluded-paths[1]=/actuator/
```

## How It Works

### Request Signing

When making server-to-server requests, include these headers:
- `X-Pushpin-Signature`: The HMAC signature
- `X-Pushpin-Timestamp`: The request timestamp (milliseconds since epoch)

The signature is calculated as:
```
HMAC(secretKey, "timestamp:path:body")
```

### Request Verification

The `HmacSignatureFilter` automatically verifies incoming requests when HMAC is enabled:
1. Checks for required headers
2. Validates request age (prevents replay attacks)
3. Verifies the signature
4. Logs audit events for failures

### Programmatic Usage

```kotlin
@Service
class MyService(private val hmacService: HmacService) {
    
    fun signRequest(body: String, path: String): Map<String, String> {
        val timestamp = System.currentTimeMillis()
        val signature = hmacService.generateRequestSignature(body, timestamp, path)
        
        return mapOf(
            hmacService.getHeaderName() to signature,
            "X-Pushpin-Timestamp" to timestamp.toString()
        )
    }
}
```

## Security Considerations

1. **Secret Key**: Use a strong, randomly generated secret key
2. **Key Rotation**: Implement a key rotation strategy
3. **Transport Security**: Always use HTTPS in production
4. **Time Synchronization**: Ensure servers have synchronized clocks
5. **Replay Protection**: Configure appropriate max-age for your use case

## Integration with Other Modules

This module integrates with:
- `pushpin-security-core`: Provides base interfaces and audit logging
- `pushpin-security-audit`: Enhanced audit trail for HMAC verification events