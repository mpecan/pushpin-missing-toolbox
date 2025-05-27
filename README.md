# Pushpin Missing Toolbox

A Spring Boot application for managing multiple Pushpin servers and allowing systems to interact with them. This project provides a simple and flexible way to integrate Pushpin into your application architecture for realtime web capabilities.

## Features

- Manage multiple Pushpin servers
- Load balancing between Pushpin servers
- Multi-server message broadcasting using ZeroMQ
- Health checks for Pushpin servers
- Comprehensive security features:
  - Multiple authentication mechanisms (token-based, JWT)
  - Role-based access control for channel operations
  - Channel-level permissions (read/write/admin)
  - Rate limiting to prevent abuse
  - HMAC signature verification for server-to-server communication
  - Encryption for sensitive channel data
  - Detailed security audit logging
- RESTful API for publishing messages
- Server-Sent Events (SSE) support for realtime updates
- Extensible server discovery system (configuration-based, AWS, Kubernetes, etc.)

## Getting Started

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose (for local development)

### Running Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/mpecan/pushpin-missing-toolbox.git
   cd pushpin-missing-toolbox
   ```

2. Start the Pushpin servers using Docker Compose:
   ```bash
   docker-compose up -d
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

4. The application will be available at http://localhost:8080

### Configuration

The application can be configured using the `application.properties` file. Here are the key configuration options:

```properties
# Pushpin server configuration
pushpin.servers[0].id=pushpin-1
pushpin.servers[0].host=pushpin-1
pushpin.servers[0].port=7999
pushpin.servers[0].controlPort=5564
pushpin.servers[0].publishPort=5560
pushpin.servers[0].active=true
pushpin.servers[0].weight=100
pushpin.servers[0].healthCheckPath=/status

# Pushpin health check configuration
pushpin.healthCheckEnabled=true
pushpin.healthCheckInterval=60000
pushpin.defaultTimeout=5000

# Pushpin authentication configuration
pushpin.authEnabled=false
pushpin.authSecret=changeme

# JWT Authentication
pushpin.security.jwt.enabled=false
pushpin.security.jwt.secret=changemechangemechangemechangemechangemechangeme
pushpin.security.jwt.issuer=pushpin-missing-toolbox
pushpin.security.jwt.audience=pushpin-client
pushpin.security.jwt.expirationMs=3600000

# Rate limiting
pushpin.security.rateLimit.enabled=false
pushpin.security.rateLimit.capacity=100
pushpin.security.rateLimit.refillTimeInMillis=60000

# Audit logging
pushpin.security.auditLogging.enabled=true
pushpin.security.auditLogging.level=INFO

# HMAC request signing for server-to-server communication
pushpin.security.hmac.enabled=false
pushpin.security.hmac.algorithm=HmacSHA256
pushpin.security.hmac.secretKey=changeme
pushpin.security.hmac.headerName=X-Pushpin-Signature

# Encryption for sensitive channel data
pushpin.security.encryption.enabled=false
pushpin.security.encryption.algorithm=AES/GCM/NoPadding
pushpin.security.encryption.secretKey=

# Multi-server ZMQ configuration
pushpin.zmqEnabled=false
pushpin.zmqHwm=1000
pushpin.zmqLinger=0

# Pushpin discovery configuration
pushpin.discovery.enabled=true
pushpin.discovery.refreshInterval=60s
pushpin.discovery.configuration.enabled=true
pushpin.discovery.aws.enabled=false
pushpin.discovery.aws.region=us-east-1
pushpin.discovery.aws.tagKey=service
pushpin.discovery.aws.tagValue=pushpin
pushpin.discovery.kubernetes.enabled=false
pushpin.discovery.kubernetes.namespace=default
pushpin.discovery.kubernetes.labelSelector=app=pushpin
```

For more details on the discovery system, see the [Discovery System Documentation](src/main/kotlin/io/github/mpecan/pmt/discovery/README.md).

## Usage

### Publishing Messages

To publish a message to a channel:

```bash
curl -X POST http://localhost:8080/api/pushpin/publish \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "test-channel",
    "data": {
      "message": "Hello, World!"
    }
  }'
```

Or using the channel-specific endpoint:

```bash
curl -X POST http://localhost:8080/api/pushpin/publish/test-channel \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, World!"
  }'
```

### Subscribing to Events

To subscribe to a channel using Server-Sent Events:

```javascript
const eventSource = new EventSource('http://localhost:8080/api/events/test-channel');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received message:', data);
};

eventSource.addEventListener('custom-event', (event) => {
  const data = JSON.parse(event.data);
  console.log('Received custom event:', data);
});

eventSource.onerror = (error) => {
  console.error('EventSource error:', error);
  eventSource.close();
};
```

### Server Management

To get information about all configured Pushpin servers:

```bash
curl http://localhost:8080/api/pushpin/servers
```

To get information about healthy Pushpin servers:

```bash
curl http://localhost:8080/api/pushpin/servers/healthy
```

## Security Features

### Authentication

The system supports multiple authentication methods:

#### Token-based Authentication

1. Set `pushpin.authEnabled=true` in the application.properties file
2. Set a secure value for `pushpin.authSecret`
3. Include the `X-Pushpin-Auth` header with the secret value in your requests

Example:

```bash
curl -X POST http://localhost:8080/api/pushpin/publish/test-channel \
  -H "Content-Type: application/json" \
  -H "X-Pushpin-Auth: your-secret-here" \
  -d '{
    "message": "Hello, World!"
  }'
```

#### JWT Authentication

For more robust authentication using JWT tokens:

1. Set `pushpin.authEnabled=true` in the application.properties file
2. Set `pushpin.security.jwt.enabled=true`
3. Configure JWT properties (provider, issuer, audience, etc.)
4. Include the JWT token in the `Authorization` header

The system supports multiple JWT providers:

##### Symmetric Key (Development/Testing)

```properties
pushpin.security.jwt.provider=symmetric
pushpin.security.jwt.secret=your-secret-key-at-least-32-chars-long
pushpin.security.jwt.issuer=pushpin-missing-toolbox
pushpin.security.jwt.audience=pushpin-client
```

For testing with the symmetric provider, you can get a token:

```bash
# Get JWT token
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your-username",
    "password": "your-password"
  }'
```

##### Keycloak Integration

```properties
pushpin.security.jwt.provider=keycloak
pushpin.security.jwt.jwksUri=https://your-keycloak-server/auth/realms/your-realm/protocol/openid-connect/certs
pushpin.security.jwt.issuer=https://your-keycloak-server/auth/realms/your-realm
pushpin.security.jwt.audience=your-client-id
```

##### Auth0 Integration

```properties
pushpin.security.jwt.provider=auth0
pushpin.security.jwt.jwksUri=https://your-auth0-tenant.auth0.com/.well-known/jwks.json
pushpin.security.jwt.issuer=https://your-auth0-tenant.auth0.com/
pushpin.security.jwt.audience=your-api-identifier
```

Using the JWT token:

```bash
# Use the JWT token to authenticate requests
curl -X POST http://localhost:8080/api/pushpin/publish/test-channel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "message": "Hello, World!"
  }'
```

### Channel Permissions

The system supports fine-grained permissions for channel operations:

- **READ**: Ability to subscribe to a channel
- **WRITE**: Ability to publish messages to a channel
- **ADMIN**: Full administrative access to a channel

Permissions can be granted to specific users or roles:

```bash
# Grant permissions to a user
curl -X POST http://localhost:8080/api/auth/permissions/user \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-jwt-token" \
  -d '{
    "username": "some-user",
    "channelId": "some-channel",
    "permissions": ["READ", "WRITE"]
  }'

# Grant permissions to a role
curl -X POST http://localhost:8080/api/auth/permissions/role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer admin-jwt-token" \
  -d '{
    "role": "ROLE_USER",
    "channelId": "some-channel",
    "permissions": ["READ"]
  }'
```

### Rate Limiting

To prevent abuse, you can enable rate limiting:

1. Set `pushpin.security.rateLimit.enabled=true`
2. Configure the capacity and refill time

The system will automatically limit the number of requests per user or IP address.

### Server-to-Server HMAC Signatures

For secure server-to-server communication, you can enable HMAC signature verification:

1. Set `pushpin.security.hmac.enabled=true`
2. Configure a secure secret key
3. Include the following headers in server-to-server requests:
   - `X-Pushpin-Signature`: HMAC signature of the request body
   - `X-Pushpin-Timestamp`: Current timestamp

### Encryption for Sensitive Channel Data

For channels that contain sensitive information, you can enable encryption:

1. Set `pushpin.security.encryption.enabled=true`
2. Configure an encryption secret key

The system will automatically encrypt message content before publishing and decrypt it when retrieving.

### Audit Logging

The system includes comprehensive audit logging for security events:

1. Set `pushpin.security.auditLogging.enabled=true`
2. Choose a logging level (INFO, DEBUG, WARN, ERROR)

Security events are logged with detailed information about the user, IP address, resource, and action.

## Multi-Server ZMQ Configuration

The application supports publishing messages to multiple Pushpin servers using ZeroMQ (ZMQ) for improved reliability and scalability. By default, this feature is disabled for backward compatibility, but it can be enabled with the following configuration:

```properties
# Enable ZMQ for multi-server communication
pushpin.zmqEnabled=true

# Choose the socket type based on your needs
# PUB (default): broadcast messages to all servers (recommended for most cases)
# PUSH: distribute messages using load balancing (one server gets each message)
pushpin.zmqSocketType=PUB

# Configure ZMQ performance parameters
pushpin.zmqHwm=1000    # High water mark (max queued messages)
pushpin.zmqLinger=0    # Linger time in ms (how long to wait for pending messages on close)
```

When ZMQ is enabled, the application will:

1. Connect to all active Pushpin servers simultaneously
2. Publish messages to all servers via ZMQ using the selected socket type
3. Ensure message delivery across the entire server cluster

This is particularly useful in high-availability setups where clients might be connected to different Pushpin instances, and you need to ensure all clients receive all messages regardless of which server they're connected to.

## Modules

### pushpin-metrics-core

The metrics core module provides a pluggable metrics collection system that automatically uses Micrometer when available on the classpath, or falls back to a no-op implementation. This allows libraries to collect metrics without forcing a dependency on Micrometer.

Features:
- Automatic implementation selection based on classpath
- Low-cardinality metrics design (no high-dimensionality tags like channel names)
- Transport-level and server-level metrics
- Spring Boot autoconfiguration support

For detailed documentation, see the pushpin-metrics-core module documentation.

## Development

### Project Structure

- `src/main/kotlin/io/github/mpecan/pmt/model` - Data models
- `src/main/kotlin/io/github/mpecan/pmt/config` - Configuration classes
- `src/main/kotlin/io/github/mpecan/pmt/service` - Services for managing Pushpin servers
- `src/main/kotlin/io/github/mpecan/pmt/controller` - REST controllers
- `src/main/kotlin/io/github/mpecan/pmt/discovery` - Extensible server discovery system
- `pushpin-metrics-core` - Metrics collection library with optional Micrometer support

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
