# Pushpin Security Encryption

This module provides encryption functionality for securing sensitive channel data in Pushpin applications.

## Features

- AES/GCM authenticated encryption
- Configurable encryption settings
- Spring Boot auto-configuration
- Interface-based design for easy customization

## Usage

Add the dependency to your project:

```kotlin
implementation(project(":pushpin-security-encryption"))
```

## Configuration

Configure encryption in your `application.yml`:

```yaml
pushpin:
  security:
    encryption:
      enabled: true  # Enable/disable encryption
      algorithm: "AES/GCM/NoPadding"  # Encryption algorithm (default)
      secret-key: ""  # Base64-encoded secret key (generate one for production!)
      key-size: 256  # Key size in bits (default: 256)
```

## Generating a Secret Key

You can generate a secret key using the `EncryptionService`:

```kotlin
@Component
class KeyGenerator(private val encryptionService: EncryptionService) {
    fun generateKey() {
        val secretKey = encryptionService.generateSecretKey()
        println("Generated secret key: $secretKey")
    }
}
```

⚠️ **Important**: Always use a strong, randomly generated secret key in production. Never commit secret keys to version control!

## Interface

The module provides the `EncryptionService` interface:

```kotlin
interface EncryptionService {
    fun encrypt(plaintext: String): String
    fun decrypt(encryptedData: String): String
    fun isEncryptionEnabled(): Boolean
    fun generateSecretKey(): String
}
```

## Default Implementation

When encryption is disabled or the module is not included, a `NoOpEncryptionService` is provided that passes data through without encryption.

## Security Considerations

1. **Key Management**: Store encryption keys securely (e.g., environment variables, secret management systems)
2. **Algorithm**: The default AES/GCM provides authenticated encryption
3. **IV Generation**: Each encryption operation uses a unique IV for security
4. **Base64 Encoding**: Encrypted data is Base64-encoded for easy transport

## Testing

The module includes comprehensive tests for both the encryption functionality and the no-op implementation.