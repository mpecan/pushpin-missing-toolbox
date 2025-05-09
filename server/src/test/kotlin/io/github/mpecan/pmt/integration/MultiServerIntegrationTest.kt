package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.WebSocketClient
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.testcontainers.PushpinContainer
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/**
 * Integration tests for multi-server message delivery.
 * 
 * These tests verify that messages are correctly delivered to clients connected to different Pushpin servers
 * using HTTP transport for message publishing.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@Testcontainers
class MultiServerIntegrationTest {

    companion object {
        private val network = Network.newNetwork()
        private val SERVER_PORT = Random.nextInt(13000, 14000)

        // Create individual Pushpin containers with shared network
        @Container
        @JvmStatic
        private val pushpinContainer1 = PushpinContainer()
            .withHostPort(SERVER_PORT)
            .withNetwork(network)
            .withNetworkAliases("pushpin-0")

        @Container
        @JvmStatic
        private val pushpinContainer2 = PushpinContainer()
            .withHostPort(SERVER_PORT)
            .withNetwork(network)
            .withNetworkAliases("pushpin-1")

        // List of containers for convenience
        private val pushpinContainers by lazy { listOf(pushpinContainer1, pushpinContainer2) }
        
        /**
         * Configure Spring Boot application properties to use multiple Pushpin containers
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Configure application to use both Pushpin servers with ZMQ disabled (using HTTP)
            TestcontainersUtils.configureMultiplePushpinProperties(
                registry,
                pushpinContainers,
                zmqEnabled = false
            )
            
            // Set server port
            registry.add("server.port") { SERVER_PORT }
            
            // Enable test mode for better diagnostics
            registry.add("pushpin.test-mode") { true }
        }
    }
    
    @Autowired
    private lateinit var pushpinService: PushpinService
    
    @Autowired
    private lateinit var discoveryManager: PushpinDiscoveryManager
    
    private val webClient = WebClient.builder().build()
    private val clients = CopyOnWriteArrayList<WebSocketClient>()
    
    @BeforeEach
    fun setUp() {
        // Ensure we can access the Pushpin servers
        val servers = discoveryManager.getAllServers()
        assert(servers.size == 2) { "Expected 2 Pushpin servers, got ${servers.size}" }
    }
    
    @AfterEach
    fun tearDown() {
        // Close all WebSocket clients
        clients.forEach { client ->
            try {
                client.closeAllConnections()
            } catch (e: Exception) {
                // Ignore exceptions during cleanup
            }
        }
        clients.clear()
    }
    
    /**
     * Tests that messages are delivered to clients connected to different Pushpin servers
     * when publishing via HTTP to multiple servers.
     */
    @Test
    fun `should deliver messages to clients connected to different servers`() {
        // Simple channel name with random UUID to avoid conflicts
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "Test message for multi-server delivery"

        // Create message collectors for verification
        val client1Messages = Collections.synchronizedList(mutableListOf<String>())
        val client2Messages = Collections.synchronizedList(mutableListOf<String>())

        // Create WebSocket clients - one for each Pushpin server
        val client1 = WebSocketClient("ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
        val client2 = WebSocketClient("ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("Created WebSocket client 1 connecting to ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("Created WebSocket client 2 connecting to ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")

        // Add clients to the cleanup list
        this.clients.add(client1)
        this.clients.add(client2)

        // Create fluxes for message consumption
        val flux1 = client1.consumeMessages("/api/ws/$channel")
        val flux2 = client2.consumeMessages("/api/ws/$channel")

        // Setup subscriptions with message capture for verification
        val subscription1 = flux1.subscribe(
            { message ->
                println("Client 1 received: $message")
                client1Messages.add(message)
            },
            { error -> println("Client 1 error: ${error.message}") }
        )

        val subscription2 = flux2.subscribe(
            { message ->
                println("Client 2 received: $message")
                client2Messages.add(message)
            },
            { error -> println("Client 2 error: ${error.message}") }
        )

        try {
            // Wait for the WebSocket connections to be established
            println("Waiting for WebSocket connections to be established...")
            Thread.sleep(2000)

            // Verify initial subscription messages were received
            assert(client1Messages.size >= 1) { "Client 1 did not receive subscription confirmation" }
            assert(client2Messages.size >= 1) { "Client 2 did not receive subscription confirmation" }
            assert(client1Messages[0].contains("Subscribed to channel")) { "Client 1 did not receive proper subscription message" }
            assert(client2Messages[0].contains("Subscribed to channel")) { "Client 2 did not receive proper subscription message" }

            // Remember the message count after subscription confirmation
            val initialSize1 = client1Messages.size
            val initialSize2 = client2Messages.size

            // Publish a message via the PushpinService (will use HTTP to all servers)
            println("Publishing message to channel: $channel")
            val message = Message.simple(channel, mapOf("text" to messageText))
            val result = pushpinService.publishMessage(message).block(Duration.ofSeconds(10))

            // Check publication result
            println("Message published successfully: $result")
            assert(result == true) { "Failed to publish message" }

            // Wait for message delivery
            println("Waiting for message delivery...")

            // Wait up to 15 seconds for message delivery
            for (i in 1..30) {
                Thread.sleep(500)

                // Print progress every few seconds
                if (i % 4 == 0) {
                    println("Still waiting for message delivery... Attempt $i")
                    println("Client 1 messages: ${client1Messages.size}, Client 2 messages: ${client2Messages.size}")
                }

                // Break early if both clients received messages
                if (client1Messages.size > initialSize1 && client2Messages.size > initialSize2) {
                    println("Both clients received messages!")
                    break
                } else if (client1Messages.size > initialSize1 || client2Messages.size > initialSize2) {
                    println("At least one client received a message. Waiting for the other...")
                }
            }

            // Print message reception results
            println("Client 1 received ${client1Messages.size - initialSize1} messages after publishing")
            println("Client 2 received ${client2Messages.size - initialSize2} messages after publishing")

            // Verify that messages were received by both clients
            if (client1Messages.size > initialSize1) {
                println("✅ Client 1 successfully received message: ${client1Messages.subList(initialSize1, client1Messages.size)}")
            } else {
                fail("Client 1 did not receive any messages after publishing")
            }

            if (client2Messages.size > initialSize2) {
                println("✅ Client 2 successfully received message: ${client2Messages.subList(initialSize2, client2Messages.size)}")
            } else {
                fail("Client 2 did not receive any messages after publishing")
            }

            println("Multi-server message delivery test completed successfully!")
        } finally {
            // Clean up subscriptions
            subscription1.dispose()
            subscription2.dispose()
        }
    }
    
    /**
     * Tests publishing multiple messages to multiple clients connected to different servers.
     */
    @Test
    fun `should deliver multiple messages to clients connected to different servers`() {
        // Channel with random UUID to avoid conflicts
        val channel = "multi-message-channel-${UUID.randomUUID()}"
        val messages = listOf(
            "First message from multi-server test",
            "Second message from multi-server test",
            "Third message from multi-server test"
        )
        
        // Create message collectors for verification
        val client1Messages = Collections.synchronizedList(mutableListOf<String>())
        val client2Messages = Collections.synchronizedList(mutableListOf<String>())

        // Create WebSocket clients - one for each Pushpin server
        val client1 = WebSocketClient("ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
        val client2 = WebSocketClient("ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("Created WebSocket clients for multiple message test")

        // Add clients to the cleanup list
        this.clients.add(client1)
        this.clients.add(client2)

        // Create fluxes for message consumption
        val flux1 = client1.consumeMessages("/api/ws/$channel")
        val flux2 = client2.consumeMessages("/api/ws/$channel")

        // Setup subscriptions with message capture for verification
        val subscription1 = flux1.subscribe(
            { message ->
                println("Client 1 (multi-message test) received: $message")
                client1Messages.add(message)
            },
            { error -> println("Client 1 error: ${error.message}") }
        )

        val subscription2 = flux2.subscribe(
            { message ->
                println("Client 2 (multi-message test) received: $message")
                client2Messages.add(message)
            },
            { error -> println("Client 2 error: ${error.message}") }
        )

        try {
            // Wait for the WebSocket connections to be established
            println("Waiting for WebSocket connections to be established...")
            Thread.sleep(2000)

            // Verify initial subscription messages were received
            assert(client1Messages.size >= 1) { "Client 1 did not receive subscription confirmation" }
            assert(client2Messages.size >= 1) { "Client 2 did not receive subscription confirmation" }

            // Remember the message count after subscription confirmation
            val initialSize1 = client1Messages.size
            val initialSize2 = client2Messages.size

            // Publish multiple messages via the PushpinService
            println("Publishing ${messages.size} messages to channel: $channel")
            
            messages.forEachIndexed { index, messageText ->
                val message = Message.simple(channel, mapOf("text" to messageText))
                val result = pushpinService.publishMessage(message).block(Duration.ofSeconds(10))
                assert(result == true) { "Failed to publish message $index: $messageText" }
                
                // Small delay between messages to ensure ordering
                Thread.sleep(500)
            }

            // Wait for message delivery
            println("Waiting for all messages to be delivered...")
            
            // Wait up to 20 seconds for message delivery
            for (i in 1..40) {
                Thread.sleep(500)

                // Print progress every few seconds
                if (i % 4 == 0) {
                    println("Waiting for message delivery... Attempt $i")
                    println("Client 1 messages: ${client1Messages.size - initialSize1}/${messages.size}, " +
                            "Client 2 messages: ${client2Messages.size - initialSize2}/${messages.size}")
                }

                // Break early if both clients received all messages
                if (client1Messages.size >= initialSize1 + messages.size && 
                    client2Messages.size >= initialSize2 + messages.size) {
                    println("Both clients received all messages!")
                    break
                }
            }

            // Print message reception results
            println("Client 1 received ${client1Messages.size - initialSize1}/${messages.size} messages")
            println("Client 2 received ${client2Messages.size - initialSize2}/${messages.size} messages")

            // Verify that all messages were received by both clients
            assert(client1Messages.size >= initialSize1 + messages.size) { 
                "Client 1 did not receive all messages. Expected ${messages.size}, got ${client1Messages.size - initialSize1}" 
            }
            assert(client2Messages.size >= initialSize2 + messages.size) { 
                "Client 2 did not receive all messages. Expected ${messages.size}, got ${client2Messages.size - initialSize2}" 
            }

            println("Multiple message delivery test completed successfully!")
        } finally {
            // Clean up subscriptions
            subscription1.dispose()
            subscription2.dispose()
        }
    }
    
    /**
     * Tests publishing messages with different formats to multiple servers.
     */
    @Test
    fun `should deliver messages with different formats to multiple servers`() {
        // Channel with random UUID to avoid conflicts
        val channel = "format-test-channel-${UUID.randomUUID()}"
        val jsonData = """{"message": "Hello from JSON", "timestamp": "${Date().time}"}"""

        // Create message collectors for verification
        val client1Messages = Collections.synchronizedList(mutableListOf<String>())
        val client2Messages = Collections.synchronizedList(mutableListOf<String>())

        // Create WebSocket clients - one for each Pushpin server
        val client1 = WebSocketClient("ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
        val client2 = WebSocketClient("ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("Created WebSocket clients for format test")

        // Add clients to the cleanup list
        this.clients.add(client1)
        this.clients.add(client2)

        // Create fluxes for message consumption
        val flux1 = client1.consumeMessages("/api/ws/$channel")
        val flux2 = client2.consumeMessages("/api/ws/$channel")

        // Setup subscriptions with message capture for verification
        val subscription1 = flux1.subscribe(
            { message ->
                println("Client 1 (format test) received: $message")
                client1Messages.add(message)
            },
            { error -> println("Client 1 error: ${error.message}") }
        )

        val subscription2 = flux2.subscribe(
            { message ->
                println("Client 2 (format test) received: $message")
                client2Messages.add(message)
            },
            { error -> println("Client 2 error: ${error.message}") }
        )

        try {
            // Wait for the WebSocket connections to be established
            println("Waiting for WebSocket connections to be established...")
            Thread.sleep(2000)

            // Verify initial subscription messages were received
            assert(client1Messages.size >= 1) { "Client 1 did not receive subscription confirmation" }
            assert(client2Messages.size >= 1) { "Client 2 did not receive subscription confirmation" }

            // Remember the message count after subscription confirmation
            val initialSize1 = client1Messages.size
            val initialSize2 = client2Messages.size

            // Publish JSON message via REST API
            println("Publishing JSON message to channel: $channel")
            val result = publishMessage(channel, jsonData, contentType = MediaType.APPLICATION_JSON)
            assert(result) { "Failed to publish JSON message" }

            // Wait for message delivery
            println("Waiting for message delivery...")
            
            // Wait up to 15 seconds for message delivery
            for (i in 1..30) {
                Thread.sleep(500)

                // Break early if both clients received messages
                if (client1Messages.size > initialSize1 && client2Messages.size > initialSize2) {
                    println("Both clients received messages!")
                    break
                }
                
                // Print progress every few seconds
                if (i % 4 == 0) {
                    println("Still waiting for message delivery... Attempt $i")
                    println("Client 1 messages: ${client1Messages.size}, Client 2 messages: ${client2Messages.size}")
                }
            }

            // Print message reception results
            println("Client 1 received ${client1Messages.size - initialSize1} messages")
            println("Client 2 received ${client2Messages.size - initialSize2} messages")

            // Verify message reception
            assert(client1Messages.size > initialSize1) { "Client 1 did not receive the JSON message" }
            assert(client2Messages.size > initialSize2) { "Client 2 did not receive the JSON message" }
            
            // Verify message content contains JSON message text
            assert(client1Messages.any { it.contains("Hello from JSON") }) { 
                "Client 1 did not receive proper message content" 
            }
            assert(client2Messages.any { it.contains("Hello from JSON") }) { 
                "Client 2 did not receive proper message content" 
            }

            println("Format test delivery completed successfully!")
        } finally {
            // Clean up subscriptions
            subscription1.dispose()
            subscription2.dispose()
        }
    }

    /**
     * Tests publishing to separate channels on different servers.
     * This test verifies that messages are only delivered to the correct channels and servers.
     */
    @Test
    fun `should deliver messages to separate channels on different servers`() {
        // Create two separate channels
        val channel1 = "test-channel-1-${UUID.randomUUID()}"
        val channel2 = "test-channel-2-${UUID.randomUUID()}"
        val message1 = "Message for channel 1 only"
        val message2 = "Message for channel 2 only"

        // Create message collectors for verification
        val client1Messages = Collections.synchronizedList(mutableListOf<String>())
        val client2Messages = Collections.synchronizedList(mutableListOf<String>())

        // Create WebSocket clients connected to different servers but subscribed to different channels
        val client1 = WebSocketClient("ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
        val client2 = WebSocketClient("ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("Created WebSocket clients for separate channel test")

        // Add clients to the cleanup list
        this.clients.add(client1)
        this.clients.add(client2)

        // Connect client1 to channel1 and client2 to channel2
        val flux1 = client1.consumeMessages("/api/ws/$channel1")
        val flux2 = client2.consumeMessages("/api/ws/$channel2")

        // Setup subscriptions with message capture for verification
        val subscription1 = flux1.subscribe(
            { message ->
                println("Client 1 (channel $channel1) received: $message")
                client1Messages.add(message)
            },
            { error -> println("Client 1 error: ${error.message}") }
        )

        val subscription2 = flux2.subscribe(
            { message ->
                println("Client 2 (channel $channel2) received: $message")
                client2Messages.add(message)
            },
            { error -> println("Client 2 error: ${error.message}") }
        )

        try {
            // Wait for the WebSocket connections to be established
            println("Waiting for WebSocket connections to be established...")
            Thread.sleep(2000)

            // Verify initial subscription messages were received
            assert(client1Messages.size >= 1) { "Client 1 did not receive subscription confirmation" }
            assert(client2Messages.size >= 1) { "Client 2 did not receive subscription confirmation" }

            // Remember the message count after subscription confirmation
            val initialSize1 = client1Messages.size
            val initialSize2 = client2Messages.size

            // Publish messages to separate channels
            println("Publishing message to channel 1: $channel1")
            val result1 = publishMessage(channel1, message1)
            assert(result1) { "Failed to publish message to channel 1" }

            println("Publishing message to channel 2: $channel2")
            val result2 = publishMessage(channel2, message2)
            assert(result2) { "Failed to publish message to channel 2" }

            // Wait for message delivery
            println("Waiting for message delivery...")
            
            // Wait up to 15 seconds for message delivery
            for (i in 1..30) {
                Thread.sleep(500)

                // Break early if both clients received messages
                if (client1Messages.size > initialSize1 && client2Messages.size > initialSize2) {
                    println("Both clients received messages on their respective channels!")
                    break
                }
                
                // Print progress every few seconds
                if (i % 4 == 0) {
                    println("Still waiting for message delivery... Attempt $i")
                    println("Client 1 (channel $channel1): ${client1Messages.size}, " +
                            "Client 2 (channel $channel2): ${client2Messages.size}")
                }
            }

            // Print message reception results
            println("Client 1 received ${client1Messages.size - initialSize1} messages on channel $channel1")
            println("Client 2 received ${client2Messages.size - initialSize2} messages on channel $channel2")

            // Verify message reception
            assert(client1Messages.size > initialSize1) { "Client 1 did not receive the message on channel $channel1" }
            assert(client2Messages.size > initialSize2) { "Client 2 did not receive the message on channel $channel2" }
            
            // Verify message content
            assert(client1Messages.any { it.contains(message1) }) { "Client 1 did not receive proper message content" }
            assert(client2Messages.any { it.contains(message2) }) { "Client 2 did not receive proper message content" }

            // Now test cross-channel isolation by creating clients for the opposite channels
            println("Testing channel isolation...")
            
            val crossClient1 = WebSocketClient("ws://localhost:${pushpinContainer1.getMappedPort(PushpinContainer.HTTP_PORT)}")
            val crossClient2 = WebSocketClient("ws://localhost:${pushpinContainer2.getMappedPort(PushpinContainer.HTTP_PORT)}")
            this.clients.add(crossClient1)
            this.clients.add(crossClient2)
            
            val crossMessages1 = Collections.synchronizedList(mutableListOf<String>())
            val crossMessages2 = Collections.synchronizedList(mutableListOf<String>())
            
            // Connect to the opposite channels
            val crossFlux1 = crossClient1.consumeMessages("/api/ws/$channel2")
            val crossFlux2 = crossClient2.consumeMessages("/api/ws/$channel1")
            
            // Setup subscriptions
            val crossSubscription1 = crossFlux1.subscribe(
                { message ->
                    println("Cross client 1 (channel $channel2) received: $message")
                    crossMessages1.add(message)
                },
                { error -> println("Cross client 1 error: ${error.message}") }
            )
            
            val crossSubscription2 = crossFlux2.subscribe(
                { message ->
                    println("Cross client 2 (channel $channel1) received: $message")
                    crossMessages2.add(message)
                },
                { error -> println("Cross client 2 error: ${error.message}") }
            )
            
            // Wait for subscription messages
            Thread.sleep(2000)
            
            // Verify subscription messages were received
            assert(crossMessages1.size >= 1) { "Cross client 1 did not receive subscription confirmation" }
            assert(crossMessages2.size >= 1) { "Cross client 2 did not receive subscription confirmation" }
            
            // Remember message counts
            val crossInitialSize1 = crossMessages1.size
            val crossInitialSize2 = crossMessages2.size
            
            // Wait to verify that no cross-channel messages were received
            Thread.sleep(3000)
            
            // Cross clients should not receive any messages after the subscription
            assert(crossMessages1.size == crossInitialSize1) { 
                "Cross client 1 received messages meant for channel 1 on channel 2" 
            }
            assert(crossMessages2.size == crossInitialSize2) { 
                "Cross client 2 received messages meant for channel 2 on channel 1" 
            }
            
            // Clean up cross subscriptions
            crossSubscription1.dispose()
            crossSubscription2.dispose()

            println("Separate channel test completed successfully!")
        } finally {
            // Clean up subscriptions
            subscription1.dispose()
            subscription2.dispose()
        }
    }
    
    /**
     * Helper method to publish a message through the REST API.
     */
    private fun publishMessage(
        channel: String,
        message: Any,
        eventType: String? = null,
        contentType: MediaType = MediaType.TEXT_PLAIN
    ): Boolean {
        val uri = if (eventType != null) {
            "http://localhost:$SERVER_PORT/api/pushpin/publish/$channel?event=$eventType"
        } else {
            "http://localhost:$SERVER_PORT/api/pushpin/publish/$channel"
        }

        val response = webClient.post()
            .uri(uri)
            .contentType(contentType)
            .bodyValue(message)
            .retrieve()
            .toBodilessEntity()
            .block(Duration.ofSeconds(5))

        return response?.statusCode?.is2xxSuccessful ?: false
    }
    
    /**
     * Helper method to fail a test with a message.
     */
    private fun fail(message: String): Nothing {
        throw AssertionError(message)
    }
}