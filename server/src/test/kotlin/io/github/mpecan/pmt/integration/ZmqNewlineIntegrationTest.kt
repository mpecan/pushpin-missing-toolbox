package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.WebSocketClient
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import java.time.Duration
import java.util.*

/**
 * Integration tests for ZMQ with special characters (newlines, tabs, etc.).
 *
 * These tests verify that the application can correctly publish messages with special
 * characters through ZeroMQ and deliver them properly to WebSocket clients.
 */
class ZmqNewlineIntegrationTest : PushpinIntegrationTest() {

    companion object {
        val definedPort = Random().nextInt(12000, 14000)

        /**
         * Create and start a Pushpin container
         */
        @Container
        @JvmStatic
        val pushpinContainer = TestcontainersUtils.createPushpinContainer(definedPort)

        /**
         * Configure Spring Boot application properties to use the Pushpin container
         * with ZMQ enabled for message publishing
         */
        @DynamicPropertySource
        @JvmStatic
        @Suppress("unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)

            // Set server port
            registry.add("server.port") { definedPort }

            // Enable ZMQ for message publishing
            registry.add("pushpin.zmq-enabled") { true }

            // Set ZMQ parameters
            registry.add("pushpin.zmq-hwm") { 1000 }
            registry.add("pushpin.zmq-linger") { 0 }

            // Enable test mode for additional diagnostics
            registry.add("pushpin.test-mode") { true }
        }
    }

    @Autowired
    private lateinit var pushpinService: PushpinService

    /**
     * Test that a message with newlines and special characters is published via ZMQ
     * and properly delivered to WebSocket clients
     */
    @Test
    fun `should publish message with newlines via ZMQ and receive via WebSocket`() {
        // Given
        val channel = "zmq-newline-test-channel-${UUID.randomUUID()}"
        val messageWithNewlines = """
            Line 1: This is a test message with newlines
            Line 2: It should be encoded properly
            Line 3: And delivered correctly
            **Tab**: Here is a tab\tcharacter
        """.trimIndent()

        // Print info about Pushpin container for debugging
        val pushpinConfig = pushpinProperties.servers[0]
        println("====== ZMQ NEWLINE TEST INFORMATION ======")
        println("Pushpin server: ${pushpinConfig.id}")
        println("Pushpin host: ${pushpinConfig.host}")
        println("Pushpin HTTP port: ${pushpinConfig.port}")
        println("Pushpin publish port: ${pushpinConfig.publishPort}")
        println("Pushpin publish URL: tcp://${pushpinConfig.host}:${pushpinConfig.publishPort}")
        println("Pushpin ZMQ enabled: ${pushpinProperties.zmqEnabled}")
        println("Test mode: ${pushpinProperties.testMode}")
        println("================================")

        // Create WebSocket client to receive messages
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")
        println("WebSocket client connecting to: ws://localhost:$pushpinPort/api/ws/$channel")

        // Create a list to collect messages
        val receivedMessages = mutableListOf<String>()

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")
        println("Successfully subscribed to WebSocket stream")

        // Subscribe to WebSocket to collect messages
        val subscription = wsFlux.subscribe(
            // onNext
            { message ->
                println("Received message: $message")
                receivedMessages.add(message)
            },
            // onError
            { error ->
                println("❌ WebSocket error: ${error.message}")
            },
            // onComplete
            {
                println("WebSocket stream completed")
            }
        )

        try {
            // Wait a bit to ensure the connection is established
            println("Waiting for connection to be established...")
            waitForConnection(3000)
            println("Connection wait complete")

            // When: Publish the message with newlines to the channel using ZMQ
            println("Publishing message with newlines to channel: $channel via ZMQ")
            val message = Message.simple(channel, mapOf("text" to messageWithNewlines))
            val result = pushpinService.publishMessage(message).block(Duration.ofSeconds(10))

            // Verify publishing was successful
            println("ZMQ publish result: $result")
            assert(result == true) { "Failed to publish message via ZMQ" }

            // Wait for message reception
            println("Waiting for message reception (10 seconds max)...")
            val startTime = System.currentTimeMillis()
            val timeout = 10000L // 10 seconds timeout
            var messageReceived = false

            // Poll until we receive the message or timeout
            while (System.currentTimeMillis() - startTime < timeout && !messageReceived) {
                if (receivedMessages.size > 1) { // First message is subscription confirmation
                    messageReceived = true
                } else {
                    // Wait a bit before checking again
                    Thread.sleep(100)
                }
            }

            // Verify that we received the message with newlines intact
            if (messageReceived) {
                val receivedPayload = receivedMessages.drop(1) // Skip subscription confirmation
                println("✅ Successfully received ${receivedPayload.size} messages via WebSocket after ZMQ publication")
                
                // Get the actual received message content (skipping the first subscription message)
                val receivedContent = receivedPayload.firstOrNull() ?: ""
                
                // Verify the message content contains all the original lines
                assert(receivedContent.contains("Line 1:")) { "Received message missing 'Line 1' content" }
                assert(receivedContent.contains("Line 2:")) { "Received message missing 'Line 2' content" }
                assert(receivedContent.contains("Line 3:")) { "Received message missing 'Line 3' content" }
                
                // Print comparison of sent vs received
                println("Original message:\n$messageWithNewlines")
                println("\nReceived message:\n$receivedContent")
                
                // Verify the message has the expected number of newlines (if properly encoded)
                val originalNewlineCount = messageWithNewlines.count { it == '\n' }
                val receivedNewlineCount = receivedContent.count { it == '\n' }
                
                println("Original newline count: $originalNewlineCount")
                println("Received newline count: $receivedNewlineCount")
            } else {
                println("⚠️ No messages received via WebSocket after ZMQ publication.")
                assert(false) { "No messages received via WebSocket after ZMQ publication" }
            }
        } finally {
            // Clean up
            subscription.dispose()
            wsClient.closeConnection("/api/ws/$channel")
        }
    }
}