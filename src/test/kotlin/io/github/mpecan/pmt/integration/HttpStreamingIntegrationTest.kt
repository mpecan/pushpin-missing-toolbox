package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.HttpStreamingClient
import io.github.mpecan.pmt.model.Transport
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
 * Integration tests for HTTP Streaming functionality.
 *
 * These tests verify that the application can correctly handle HTTP Streaming connections
 * and publish/receive messages through Pushpin using plain HTTP streaming (not SSE).
 */
class HttpStreamingIntegrationTest : PushpinIntegrationTest() {
    companion object {
        val definedPort = Random().nextInt(10000, 12000)

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
    fun `should receive message via HTTP Streaming`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "Hello from HTTP Streaming test!"

        // Create an HTTP Streaming client that connects to the streaming endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val httpStreamingClient = HttpStreamingClient("http://localhost:$pushpinPort")

        // Subscribe to the HTTP stream
        val streamFlux = httpStreamingClient.consumeStream("/api/http-stream/$channel")

        // Use StepVerifier to test the HTTP stream
        val stepVerifier = StepVerifier.create(streamFlux)
            .expectNextMatches { it.startsWith("Successfully subscribed to channel: $channel") }
            .expectNext(messageText)
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish a message to the channel
        publishMessageWithTransports(channel, messageText, listOf(Transport.HttpStream))

        // Then: Verify that the message was received via HTTP Streaming
        stepVerifier.verify(Duration.ofSeconds(10))
    }

    @Test
    fun `should receive multiple messages via HTTP Streaming`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val message1 = "First HTTP Streaming message"
        val message2 = "Second HTTP Streaming message"
        val message3 = "Third HTTP Streaming message"

        // Create an HTTP Streaming client that connects to the streaming endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val httpStreamingClient = HttpStreamingClient("http://localhost:$pushpinPort")

        // Subscribe to the HTTP stream
        val streamFlux = httpStreamingClient.consumeStream("/api/http-stream/$channel")

        // Use StepVerifier to test the HTTP stream
        val stepVerifier = StepVerifier.create(streamFlux)
            .expectNextMatches { it.startsWith("Successfully subscribed to channel: $channel") }
            .expectNext(message1)
            .expectNext(message2)
            .expectNext(message3)
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish multiple messages to the channel
        publishMessageWithTransports(channel, message1, listOf(Transport.HttpStream))
        publishMessageWithTransports(channel, message2, listOf(Transport.HttpStream))
        publishMessageWithTransports(channel, message3, listOf(Transport.HttpStream))

        // Then: Verify that all messages were received via HTTP Streaming
        stepVerifier.verify(Duration.ofSeconds(10))
    }
}
