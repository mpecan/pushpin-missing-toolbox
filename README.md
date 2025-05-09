# Pushpin Missing Toolbox

A Spring Boot application for managing multiple Pushpin servers and allowing systems to interact with them. This project provides a simple and flexible way to integrate Pushpin into your application architecture for realtime web capabilities.

## Features

- Manage multiple Pushpin servers
- Load balancing between Pushpin servers
- Multi-server message broadcasting using ZeroMQ
- Health checks for Pushpin servers
- Authentication mechanism for secure communication
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

# Multi-server ZMQ configuration
pushpin.zmqEnabled=false
pushpin.zmqSocketType=PUB
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

## Authentication

By default, authentication is disabled. To enable authentication:

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

## Development

### Project Structure

- `src/main/kotlin/io/github/mpecan/pmt/model` - Data models
- `src/main/kotlin/io/github/mpecan/pmt/config` - Configuration classes
- `src/main/kotlin/io/github/mpecan/pmt/service` - Services for managing Pushpin servers
- `src/main/kotlin/io/github/mpecan/pmt/controller` - REST controllers
- `src/main/kotlin/io/github/mpecan/pmt/discovery` - Extensible server discovery system

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
