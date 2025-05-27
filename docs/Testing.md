# Testing Guide

This guide covers testing strategies and tools for the Pushpin Missing Toolbox, including integration testing with Testcontainers.

## Table of Contents

- [Testing Overview](#testing-overview)
- [Testcontainers Module](#testcontainers-module)
- [Integration Testing](#integration-testing)
- [Unit Testing](#unit-testing)
- [Performance Testing](#performance-testing)
- [Testing Patterns](#testing-patterns)
- [CI/CD Integration](#cicd-integration)

## Testing Overview

The Pushpin Missing Toolbox uses a comprehensive testing strategy that includes:

- **Unit Tests** - Testing individual components in isolation
- **Integration Tests** - Testing component interactions with real Pushpin servers
- **Testcontainers** - Containerized Pushpin instances for realistic testing
- **Performance Tests** - Load testing and benchmarking
- **End-to-End Tests** - Complete workflow validation

### Test Structure

```
project/
├── server/src/test/kotlin/
│   ├── unit/                    # Unit tests
│   ├── integration/             # Integration tests with Testcontainers
│   ├── mock/                    # Mock-based tests
│   └── testcontainers/          # Testcontainer utilities
├── pushpin-testcontainers/      # Pushpin Testcontainers module
├── */src/test/kotlin/           # Module-specific tests
└── docs/Testing.md              # This file
```

## Testcontainers Module

The `pushpin-testcontainers` module provides a comprehensive Testcontainers implementation for Pushpin, making it easy to write integration tests.

### Key Features

- **Full Pushpin Configuration** - All configuration options supported
- **Multiple Container Support** - Test multi-server scenarios
- **Spring Boot Integration** - Automatic property configuration
- **Configuration Presets** - Optimized settings for different use cases
- **Network Support** - Multi-container networking for complex scenarios

### Quick Start

Add the dependency to your test module:

```kotlin
dependencies {
    testImplementation(project(":pushpin-testcontainers"))
}
```

Create a simple test:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class MyIntegrationTest {
    
    companion object {
        private val SERVER_PORT = 8080
        
        @Container
        @JvmStatic
        val pushpinContainer = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
            registry.add("server.port") { SERVER_PORT }
        }
    }
    
    @Test
    fun `should publish messages successfully`() {
        // Your test code here
    }
}
```

### Available Presets

The module includes several configuration presets for common scenarios:

```kotlin
// Basic HTTP testing
PushpinContainerBuilder()
    .withPreset(PushpinPresets.minimal())
    .build()

// WebSocket optimized
PushpinContainerBuilder()
    .withPreset(PushpinPresets.webSocket())
    .build()

// Server-Sent Events
PushpinContainerBuilder()
    .withPreset(PushpinPresets.serverSentEvents())
    .build()

// High throughput testing
PushpinContainerBuilder()
    .withPreset(PushpinPresets.highThroughput())
    .build()

// Security testing
PushpinContainerBuilder()
    .withPreset(PushpinPresets.authenticated())
    .build()

// Production-like setup
PushpinContainerBuilder()
    .withPreset(PushpinPresets.productionLike())
    .build()

// Maximum debugging
PushpinContainerBuilder()
    .withPreset(PushpinPresets.development())
    .build()
```

## Integration Testing

### Single Server Tests

Test basic functionality with a single Pushpin server:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class SingleServerIntegrationTest {
    
    companion object {
        @Container
        @JvmStatic
        val pushpinContainer = TestcontainersUtils.createPushpinContainer(8080)
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
            registry.add("server.port") { 8080 }
        }
    }
    
    @Autowired
    private lateinit var pushpinService: PushpinService
    
    @Test
    fun `should publish and receive messages`() {
        val channel = "test-channel"
        val message = Message.simple(channel, mapOf("text" to "Hello"))
        
        val result = pushpinService.publishMessage(message).block()
        
        assertThat(result).isTrue()
    }
}
```

### Multi-Server Tests

Test load balancing and failover scenarios:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class MultiServerIntegrationTest {
    
    companion object {
        private val network = Network.newNetwork()
        private val SERVER_PORT = 8080
        
        @Container
        @JvmStatic
        private val pushpinContainer1 = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
            .withNetwork(network)
            .withNetworkAliases("pushpin-1")
        
        @Container
        @JvmStatic
        private val pushpinContainer2 = PushpinContainerBuilder()
            .withHostApplicationPort(SERVER_PORT)
            .withSimpleHostRoute()
            .build()
            .withNetwork(network)
            .withNetworkAliases("pushpin-2")
        
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configureMultiplePushpinProperties(
                registry,
                listOf(pushpinContainer1, pushpinContainer2),
                zmqEnabled = true
            )
            registry.add("server.port") { SERVER_PORT }
        }
    }
    
    @Test
    fun `should distribute messages to all servers`() {
        // Test that messages reach clients connected to different servers
    }
}
```

### WebSocket Testing

Test real-time WebSocket connections:

```kotlin
@Test
fun `should handle WebSocket connections`() {
    val channel = "ws-test-channel"
    val receivedMessages = mutableListOf<String>()
    
    // Create WebSocket client
    val client = WebSocketClient("ws://localhost:${pushpinContainer.getHttpPort()}")
    val flux = client.consumeMessages("/api/ws/$channel")
    
    // Subscribe to messages
    val subscription = flux.subscribe { message ->
        receivedMessages.add(message)
    }
    
    try {
        // Wait for connection
        Thread.sleep(1000)
        
        // Publish message
        val message = Message.simple(channel, mapOf("data" to "WebSocket test"))
        pushpinService.publishMessage(message).block()
        
        // Wait for delivery
        Thread.sleep(1000)
        
        // Verify message received
        assertThat(receivedMessages).isNotEmpty()
        assertThat(receivedMessages.last()).contains("WebSocket test")
        
    } finally {
        subscription.dispose()
        client.closeAllConnections()
    }
}
```

### Server-Sent Events Testing

Test SSE streams:

```kotlin
@Test
fun `should stream Server-Sent Events`() {
    val channel = "sse-test-channel"
    
    // Start SSE connection
    val webClient = WebClient.builder().build()
    val eventFlux = webClient.get()
        .uri("http://localhost:${pushpinContainer.getHttpPort()}/api/events/$channel")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(String::class.java)
    
    val receivedEvents = mutableListOf<String>()
    val subscription = eventFlux.subscribe { event ->
        receivedEvents.add(event)
    }
    
    try {
        // Wait for subscription
        Thread.sleep(1000)
        
        // Publish events
        val message1 = Message.simple(channel, mapOf("event" to "first"))
        val message2 = Message.simple(channel, mapOf("event" to "second"))
        
        pushpinService.publishMessage(message1).block()
        pushpinService.publishMessage(message2).block()
        
        // Wait for delivery
        Thread.sleep(2000)
        
        // Verify events received
        assertThat(receivedEvents.size).isGreaterThanOrEqualTo(2)
        
    } finally {
        subscription.dispose()
    }
}
```

## Unit Testing

### Service Layer Tests

Test business logic in isolation:

```kotlin
class PushpinServiceTest {
    
    @Mock
    private lateinit var discoveryManager: PushpinDiscoveryManager
    
    @Mock
    private lateinit var messageFormatter: MessageFormatter<*>
    
    @InjectMocks
    private lateinit var pushpinService: PushpinService
    
    @Test
    fun `should handle server failure gracefully`() {
        // Mock server failure scenario
        val server = PushpinServer("test", "localhost", 7999)
        whenever(discoveryManager.getHealthyServers()).thenReturn(emptyList())
        
        val message = Message.simple("test", mapOf("data" to "test"))
        val result = pushpinService.publishMessage(message).block()
        
        assertThat(result).isFalse()
    }
}
```

### Controller Tests

Test API endpoints:

```kotlin
@WebMvcTest(PushpinController::class)
class PushpinControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var pushpinService: PushpinService
    
    @Test
    fun `should publish message via REST API`() {
        whenever(pushpinService.publishMessage(any())).thenReturn(Mono.just(true))
        
        mockMvc.perform(
            post("/api/pushpin/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"channel": "test", "data": {"message": "hello"}}""")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
    }
}
```

## Performance Testing

### Load Testing

Test high-throughput scenarios:

```kotlin
@Test
fun `should handle high message volume`() {
    val channel = "load-test-channel"
    val messageCount = 1000
    val concurrency = 10
    
    // Use high-throughput preset
    val container = PushpinContainerBuilder()
        .withPreset(PushpinPresets.highThroughput())
        .withHostApplicationPort(8080)
        .build()
    
    // Configure for performance
    container.start()
    
    val executor = Executors.newFixedThreadPool(concurrency)
    val latch = CountDownLatch(messageCount)
    val errors = AtomicInteger(0)
    
    try {
        repeat(messageCount) { i ->
            executor.submit {
                try {
                    val message = Message.simple(channel, mapOf("id" to i))
                    val result = pushpinService.publishMessage(message).block()
                    if (result != true) errors.incrementAndGet()
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // Wait for completion (max 30 seconds)
        val completed = latch.await(30, TimeUnit.SECONDS)
        
        assertThat(completed).isTrue()
        assertThat(errors.get()).isLessThan(messageCount * 0.01) // < 1% error rate
        
    } finally {
        executor.shutdown()
        container.stop()
    }
}
```

### Memory Usage Testing

Monitor resource usage during tests:

```kotlin
@Test
fun `should not leak memory during long-running operations`() {
    val runtime = Runtime.getRuntime()
    val initialMemory = runtime.totalMemory() - runtime.freeMemory()
    
    // Perform many operations
    repeat(10000) { i ->
        val message = Message.simple("memory-test", mapOf("iteration" to i))
        pushpinService.publishMessage(message).block()
        
        if (i % 1000 == 0) {
            System.gc()
            Thread.sleep(100)
        }
    }
    
    System.gc()
    Thread.sleep(1000)
    
    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val memoryIncrease = finalMemory - initialMemory
    
    // Memory increase should be reasonable (less than 50MB)
    assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024)
}
```

## Testing Patterns

### Test Data Builders

Create reusable test data:

```kotlin
object TestDataBuilder {
    
    fun pushpinServer(
        id: String = "test-server",
        host: String = "localhost",
        port: Int = 7999
    ) = PushpinServer(
        id = id,
        host = host,
        port = port,
        controlPort = port + 4,
        publishPort = port - 439,
        active = true
    )
    
    fun message(
        channel: String = "test-channel",
        data: Any = mapOf("test" to "data")
    ) = Message.simple(channel, data)
    
    fun containerBuilder() = PushpinContainerBuilder()
        .withPreset(PushpinPresets.minimal())
        .withHostApplicationPort(8080)
}
```

### Custom Test Annotations

Create reusable test configurations:

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@TestMethodOrder(OrderAnnotation::class)
annotation class PushpinIntegrationTest

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase
annotation class PushpinUnitTest
```

### Test Utilities

Common testing utilities:

```kotlin
object TestUtils {
    
    fun waitFor(
        condition: () -> Boolean,
        timeout: Duration = Duration.ofSeconds(10),
        interval: Duration = Duration.ofMillis(100)
    ): Boolean {
        val deadline = Instant.now().plus(timeout)
        
        while (Instant.now().isBefore(deadline)) {
            if (condition()) return true
            Thread.sleep(interval.toMillis())
        }
        
        return false
    }
    
    fun randomPort(): Int = Random.nextInt(10000, 20000)
    
    fun uniqueChannel(): String = "test-${UUID.randomUUID()}"
}
```

## CI/CD Integration

### GitHub Actions

Example workflow for testing:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      docker:
        image: docker:20.10.7
        options: --privileged
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    
    - name: Run unit tests
      run: ./gradlew test
    
    - name: Run integration tests
      run: ./gradlew integrationTest
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Test Results
        path: '**/build/test-results/test/TEST-*.xml'
        reporter: java-junit
```

### Docker Compose for Testing

Testing environment with multiple Pushpin servers:

```yaml
# docker-compose.test.yml
version: '3.8'

services:
  pushpin-1:
    image: fanout/pushpin:1.40.1
    ports:
      - "7999:7999"
    volumes:
      - ./config/pushpin-1:/etc/pushpin
  
  pushpin-2:
    image: fanout/pushpin:1.40.1
    ports:
      - "7998:7999"
    volumes:
      - ./config/pushpin-2:/etc/pushpin
  
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - PUSHPIN_SERVERS_0_HOST=pushpin-1
      - PUSHPIN_SERVERS_1_HOST=pushpin-2
    depends_on:
      - pushpin-1
      - pushpin-2
```

### Test Configuration

Separate configuration for testing:

```yaml
# application-test.yml
server:
  port: 8080

pushpin:
  test-mode: true
  health-check-enabled: true
  health-check-interval: 1000
  default-timeout: 5000
  
logging:
  level:
    io.github.mpecan.pmt: DEBUG
    org.testcontainers: INFO

spring:
  main:
    lazy-initialization: true
```

## Best Practices

### Container Management

1. **Reuse Containers** - Use `@Container` with `@JvmStatic` for class-level containers
2. **Network Isolation** - Use custom networks for multi-container tests
3. **Resource Cleanup** - Always clean up WebSocket connections and subscriptions
4. **Port Management** - Use dynamic ports to avoid conflicts

### Test Organization

1. **Separate Unit and Integration Tests** - Different test source sets
2. **Use Descriptive Names** - Test names should describe the scenario
3. **Group Related Tests** - Use inner classes or test suites
4. **Document Complex Scenarios** - Add comments for complex test setups

### Performance

1. **Parallel Execution** - Run independent tests in parallel
2. **Container Caching** - Reuse containers when possible
3. **Selective Testing** - Use tags to run specific test categories
4. **Resource Monitoring** - Monitor memory and CPU usage in tests

### Debugging

1. **Enable Debug Logging** - Use debug presets for troubleshooting
2. **Container Logs** - Access container logs for debugging
3. **Test Timeouts** - Set appropriate timeouts for async operations
4. **Test Data** - Use meaningful test data for easier debugging

## Troubleshooting

### Common Issues

**Container Won't Start**
```bash
# Check Docker daemon
docker info

# Check port conflicts
netstat -tulpn | grep :7999

# Check container logs
docker logs <container_id>
```

**Tests Timeout**
```kotlin
// Increase timeouts for slower environments
@Test(timeout = 30000)
fun `long running test`() {
    // Test with extended timeout
}
```

**Port Conflicts**
```kotlin
// Use random ports
private val SERVER_PORT = Random.nextInt(10000, 20000)
```

**Memory Issues**
```kotlin
// Limit container resources
val container = PushpinContainerBuilder()
    .withConfiguration { 
        copy(
            clientMaxConn = 1000,
            messageHwm = 10000
        ) 
    }
    .build()
```

For more detailed information about the Testcontainers module, see the [pushpin-testcontainers README](../pushpin-testcontainers/README.md).