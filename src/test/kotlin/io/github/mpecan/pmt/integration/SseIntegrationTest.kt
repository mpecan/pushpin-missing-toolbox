package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.SseClient
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

/**
 * Integration tests for Server-Sent Events (SSE) functionality.
 *
 * These tests verify that the application can correctly handle SSE connections
 * and publish/receive messages through Pushpin using the SSE protocol.
 */
@AutoConfigureWebTestClient
class SseIntegrationTest : PushpinIntegrationTest() {
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
    fun `should receive message via SSE`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "Hello from SSE test!"

        // Create an SSE client that connects to the events endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val sseClient = SseClient("http://localhost:$pushpinPort")

        // Subscribe to the SSE stream
        val sseFlux = sseClient.consumeEvents("/api/events/$channel")

        // Use StepVerifier to test the SSE stream
        val stepVerifier = StepVerifier.create(sseFlux)
            .expectNext(
                ServerSentEvent.builder(
                    "Successfully subscribed to channel: $channel"
                ).build()
            )
            .expectNext(
                ServerSentEvent.builder(messageText).build()
            )
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish a message to the channel
        publishMessage(channel, messageText)

        // Then: Verify that the message was received via SSE
        stepVerifier.verify(Duration.ofSeconds(10))
    }

    @Test
    fun `should receive multiple messages via SSE`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val message1 = "First SSE message"
        val message2 = "Second SSE message"
        val message3 = "Third SSE message"

        // Create an SSE client that connects to the events endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val sseClient = SseClient("http://localhost:$pushpinPort")

        // Subscribe to the SSE stream
        val sseFlux = sseClient.consumeEvents("/api/events/$channel")

        // Use StepVerifier to test the SSE stream
        val stepVerifier = StepVerifier.create(sseFlux)
            .expectNext(
                ServerSentEvent.builder(
                    "Successfully subscribed to channel: $channel"
                ).build()
            )
            .expectNext(
                ServerSentEvent.builder(message1.replace("", "")).build()
            )
            .expectNext(
                ServerSentEvent.builder(message2.replace("", "")).build()
            )
            .expectNext(
                ServerSentEvent.builder(message3.replace("", "")).build()
            )
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish multiple messages to the channel
        publishMessage(channel, message1)
        publishMessage(channel, message2)
        publishMessage(channel, message3)

        // Then: Verify that all messages were received via SSE
        stepVerifier.verify(Duration.ofSeconds(10))
    }

    @Test
    fun `should receive message with event type via SSE`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val eventType = "custom-event"
        val messageText = "Hello with custom event type!"

        // Create an SSE client that connects to the events endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val sseClient = SseClient("http://localhost:$pushpinPort")

        // Subscribe to the SSE stream
        val sseFlux = sseClient.consumeEvents("/api/events/$channel")

        // Use StepVerifier to test the SSE stream
        val stepVerifier = StepVerifier.create(sseFlux)
            .expectNext(
                ServerSentEvent.builder(
                    "Successfully subscribed to channel: $channel"
                ).build()
            )
            .expectNext(
                ServerSentEvent.builder(messageText)
                    .event(eventType)
                    .build()
            )
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish a message with event type to the channel
        publishMessage(channel, messageText, eventType)

        // Then: Verify that the message was received via SSE with the correct event type
        stepVerifier.verify(Duration.ofSeconds(10))
    }
}
