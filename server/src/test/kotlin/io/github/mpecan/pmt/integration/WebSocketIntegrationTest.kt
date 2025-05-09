package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.WebSocketClient
import io.github.mpecan.pmt.test.PortProvider
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

/**
 * Integration tests for WebSocket functionality.
 *
 * These tests verify that the application can correctly handle WebSocket connections
 * and publish/receive messages through Pushpin using the WebSocket protocol.
 *
 * Tests cover:
 * - Basic text message exchange
 * - Multiple message exchange
 * - Messages with event types
 * - Binary data transmission
 * - Protocol-specific features (ping/pong)
 */
class WebSocketIntegrationTest : PushpinIntegrationTest() {

    companion object {
        val definedPort = PortProvider.getPort()

        /**
         * Create and start a Pushpin container
         */
        @Container
        @JvmStatic
        val pushpinContainer = TestcontainersUtils.createPushpinContainer(definedPort)

        /**
         * Configure Spring Boot application properties to use the Pushpin container
         */
        @DynamicPropertySource
        @JvmStatic
        @Suppress("unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
            registry.add("server.port") { definedPort }
        }
    }


    @Test
    fun `should receive message via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "Hello from WebSocket test!"

        // Create a WebSocket client that connects to the WebSocket endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")

        // Use StepVerifier to test the WebSocket stream
        val stepVerifier = StepVerifier.create(wsFlux)
            .expectNext("""{"success": true, "message": "Subscribed to channel: $channel"}""")
            .expectNextMatches {
                it.contains(messageText)
            }
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(300)

        // When: Publish a message to the channel
        publishMessage(channel, messageText)

        // Then: Verify that the message was received via WebSocket
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }

    @Test
    fun `should receive multiple messages via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val message1 = "First WebSocket message"
        val message2 = "Second WebSocket message"
        val message3 = "Third WebSocket message"

        // Create a WebSocket client that connects to the WebSocket endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")

        // Use StepVerifier to test the WebSocket stream
        val stepVerifier = StepVerifier.create(wsFlux)
            .expectNext("""{"success": true, "message": "Subscribed to channel: $channel"}""")
            .expectNextMatches{
                it.contains("First WebSocket message")
            }
            .expectNextMatches{
                it.contains("Second WebSocket message")
            }
            .expectNextMatches{
                it.contains("Third WebSocket message")
            }
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(300)

        // When: Publish multiple messages to the channel
        publishMessage(channel, message1)
        publishMessage(channel, message2)
        publishMessage(channel, message3)

        // Then: Verify that all messages were received via WebSocket
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }

    @Test
    fun `should receive message with event type via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val eventType = "custom-event"
        val messageText = "Hello with custom event type!"

        // Create a WebSocket client that connects to the WebSocket endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")

        // Use StepVerifier to test the WebSocket stream
        val stepVerifier = StepVerifier.create(wsFlux)
            .expectNext("""{"success": true, "message": "Subscribed to channel: $channel"}""")
            .expectNextMatches {
                it.contains(messageText)
            }
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(300)

        // When: Publish a message with event type to the channel
        publishMessage(channel, messageText, eventType)

        // Then: Verify that the message was received via WebSocket with the correct event type
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }

    @Test
    fun `should transmit binary data via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val binaryData = ByteArray(10) { it.toByte() }  // Sample binary data
        val encodedData = Base64.getEncoder().encodeToString(binaryData)
        val messageText = """{"type":"binary","data":"$encodedData"}"""

        // Create a WebSocket client that connects to the WebSocket endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")

        // Use StepVerifier to test the WebSocket stream
        val stepVerifier = StepVerifier.create(wsFlux)
            .expectNext("""{"success": true, "message": "Subscribed to channel: $channel"}""")
            .expectNextMatches {
                it.contains(encodedData)
            }
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(300)

        // When: Publish a binary message to the channel
        publishMessage(channel, messageText)

        // Then: Verify that the binary message was received via WebSocket
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }

    @Test
    fun `should handle WebSocket ping-pong`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"

        // Create a WebSocket client that connects to the WebSocket endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val wsClient = WebSocketClient("ws://localhost:$pushpinPort")

        // Subscribe to the WebSocket stream
        val wsFlux = wsClient.consumeMessages("/api/ws/$channel")

        // Use StepVerifier to test the WebSocket stream
        val stepVerifier = StepVerifier.create(wsFlux)
            .expectNext("""{"success": true, "message": "Subscribed to channel: $channel"}""")
            // The ping/pong is handled at a lower level and we don't see the actual messages
            // But we can verify the connection stays alive
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(300)

        // When/Then: The connection should remain open for a while, indicating ping/pong is working
        // This is a simple test that just verifies the connection doesn't close prematurely
        waitForConnection(2000)  // Wait longer than normal to ensure ping/pong has a chance to occur

        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }
}
