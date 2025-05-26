# Pushpin ZMQ Transport

This module provides ZeroMQ transport functionality for publishing messages to Pushpin servers.

## Components

### ZmqTransport
- **Purpose**: Publishes messages to Pushpin servers via ZeroMQ
- **Features**: 
  - Connection pooling for improved performance
  - PUSH socket type (compatible with Pushpin's PULL sockets)
  - Configurable socket parameters (HWM, linger, timeouts)
  - Automatic reconnection handling
  - Reactive and blocking APIs

### ZmqTransportProperties
- **Purpose**: Configuration properties for ZMQ transport
- **Configurable Properties**:
  - `connectionPoolEnabled`: Enable/disable connection pooling
  - `hwm`: High water mark for ZMQ sockets
  - `linger`: Linger time in milliseconds
  - `sendTimeout`: Send timeout in milliseconds
  - `reconnectIvl`: Reconnection interval
  - `reconnectIvlMax`: Maximum reconnection interval
  - `connectionPoolRefreshInterval`: Pool refresh interval

### Auto-Configuration
- **ZmqTransportAutoConfiguration**: Automatically configures ZMQ transport components
- **Auto-discovery**: Automatically detected by Spring Boot applications

## Configuration

Configure via application properties:

```properties
# ZMQ Transport configuration
pushpin.transport.zmq.connectionPoolEnabled=true
pushpin.transport.zmq.hwm=1000
pushpin.transport.zmq.linger=0
pushpin.transport.zmq.sendTimeout=1000
pushpin.transport.zmq.reconnectIvl=100
pushpin.transport.zmq.reconnectIvlMax=0
pushpin.transport.zmq.connectionPoolRefreshInterval=60000
```

## Usage

Add the dependency to your project:

```kotlin
implementation(project(":pushpin-transport-zmq"))
```

The ZMQ transport will be automatically configured and available for injection:

```kotlin
@Service
class MyService(private val zmqTransport: ZmqTransport) {
    fun publishMessage() {
        val message = Message.simple("channel", "data")
        val servers = listOf(PushpinServer("server1", "localhost", 7999, 5564, 5560))
        
        // Reactive API
        zmqTransport.publishReactive(servers, message)
            .subscribe { success -> 
                println("Published: $success")
            }
            
        // Blocking API
        val futures = zmqTransport.publish(servers, message)
        futures.forEach { future ->
            val success = future.get()
            println("Published: $success")
        }
    }
}
```

## Architecture

The ZMQ transport uses:
- **PUSH sockets** to connect to Pushpin's PULL sockets
- **Connection pooling** to reuse sockets across requests
- **Thread-safe** socket management
- **Automatic cleanup** on application shutdown