# Pushpin Security JWT

This module provides JWT (JSON Web Token) functionality for authentication and claim extraction in the Pushpin ecosystem.

## Features

- JWT decoder for multiple providers (symmetric, OAuth2, Keycloak, Auth0, Okta)
- Flexible claim extraction using JsonPath
- Channel subscription extraction from JWT tokens
- Spring Security integration
- Spring Boot AutoConfiguration

## Usage

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.mpecan:pushpin-security-jwt")
}
```

## Configuration

Configure JWT in your `application.properties` or `application.yml`:

```properties
# Enable JWT authentication
pushpin.security.jwt.enabled=true

# JWT provider type
pushpin.security.jwt.provider=symmetric

# For symmetric key provider
pushpin.security.jwt.secret=your-secret-key-at-least-32-characters

# For OAuth2 providers (keycloak, auth0, okta, oauth2)
pushpin.security.jwt.jwks-uri=https://your-provider.com/.well-known/jwks.json

# JWT validation settings
pushpin.security.jwt.issuer=https://your-issuer.com
pushpin.security.jwt.audience=your-audience

# Authorities configuration
pushpin.security.jwt.authorities-claim=scope
pushpin.security.jwt.authorities-prefix=SCOPE_

# Claim extraction for channels
pushpin.security.jwt.claim-extraction.enabled=true
pushpin.security.jwt.claim-extraction.extract-claims[0]=$.channels
```

## Supported JWT Providers

### Symmetric Key Provider
For development and testing:
```properties
pushpin.security.jwt.provider=symmetric
pushpin.security.jwt.secret=your-secret-key-at-least-32-characters-long
```

### OAuth2 Providers
For production with external identity providers:
```properties
# Keycloak
pushpin.security.jwt.provider=keycloak
pushpin.security.jwt.jwks-uri=https://keycloak.example.com/auth/realms/your-realm/protocol/openid-connect/certs

# Auth0
pushpin.security.jwt.provider=auth0
pushpin.security.jwt.jwks-uri=https://your-domain.auth0.com/.well-known/jwks.json

# Okta
pushpin.security.jwt.provider=okta
pushpin.security.jwt.jwks-uri=https://your-domain.okta.com/oauth2/default/v1/keys

# Generic OAuth2
pushpin.security.jwt.provider=oauth2
pushpin.security.jwt.jwks-uri=https://your-provider.com/.well-known/jwks.json
```

## Channel Subscription Extraction

The module can extract channel subscriptions from JWT tokens in multiple formats:

### Array Format
```json
{
  "channels": ["channel1", "channel2", "news.*"]
}
```

### Object Array Format
```json
{
  "channels": [
    { "id": "channel1", "expires": "2024-12-31" },
    { "id": "channel2", "metadata": "value" }
  ]
}
```

### Map Format
```json
{
  "channels": {
    "channel1": { "expires": "2024-12-31" },
    "channel2": { "metadata": "value" }
  }
}
```

## Programmatic Usage

### JWT Decoder Service
```kotlin
@Service
class MyService(private val jwtDecoderService: JwtDecoderService) {
    
    fun validateToken(token: String) {
        val decoder = jwtDecoderService.getDecoder()
        val jwt = decoder.decode(token)
        // Process JWT...
    }
}
```

### Claim Extraction
```kotlin
@Service
class MyService(private val claimExtractorService: ClaimExtractorService) {
    
    fun extractUserRoles(jwt: Jwt): List<String> {
        return claimExtractorService.extractListClaim(jwt, "$.roles")
    }
}
```

### Channel Subscription Extraction
```kotlin
@Service
class MyService(private val channelExtractorService: ChannelSubscriptionExtractorService) {
    
    fun getUserChannels(jwt: Jwt): ChannelSubscriptions? {
        return channelExtractorService.extractChannelSubscriptions(jwt)
    }
}
```

## Security Considerations

1. **Secret Key Security**: Use strong, randomly generated secrets for symmetric keys
2. **Key Rotation**: Implement proper key rotation strategies
3. **JWKS Caching**: JWKS endpoints are cached by Spring Security
4. **Token Validation**: Always validate issuer, audience, and expiration
5. **Claim Validation**: Validate extracted claims before using them

## Integration with Other Modules

This module integrates with:
- `pushpin-security-core`: Provides base interfaces and models
- `pushpin-security-audit`: Logs authentication events
- Spring Security: Provides OAuth2 resource server capabilities