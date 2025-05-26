package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.client.LongPollingClient
import io.github.mpecan.pmt.test.PortProvider
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

/**
 * Integration tests for HTTP Long-Polling functionality.
 *
 * These tests verify that the application can correctly handle HTTP Long-Polling connections
 * and publish/receive messages through Pushpin using the Long-Polling protocol.
 */
class LongPollingIntegrationTest : PushpinIntegrationTest() {
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
    fun `should receive message via Long-Polling`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = mapOf("message" to "Hello from Long-Polling test!")

        // Create a Long-Polling client that connects to the long-polling endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val longPollingClient = LongPollingClient("http://localhost:$pushpinPort")

        // First, set up a poll
        val pollMono = longPollingClient.consumeMessages("/api/long-polling/$channel")

        val stepVerifier = StepVerifier.create(pollMono)
            .expectNextMatches { response ->
                (
                    response.containsKey("channel") && response["channel"] == channel &&
                        response["message"].toString().contains("Hello from Long-Polling test!")
                    ) ||
                    (
                        response.containsKey("message") &&
                            response["message"].toString()
                                .contains("Hello from Long-Polling test!")
                        )
            }
            .thenCancel()
            .verifyLater()
        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // When: Publish a message to the channel
        publishMessage(channel, mapOf("message" to messageText), contentType = MediaType.APPLICATION_JSON)

        // Then: Verify that the message was received via Long-Polling
        stepVerifier.verify(Duration.ofSeconds(30))
    }

    @Test
    fun `should receive multiple messages via Long-Polling`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val message1 = mapOf("message" to "First Long-Polling message")
        val message2 = mapOf("message" to "Second Long-Polling message")
        val message3 = mapOf("message" to "Third Long-Polling message")

        // Create a Long-Polling client that connects to the long-polling endpoint through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val longPollingClient = LongPollingClient("http://localhost:$pushpinPort")

        // For the first message
        // Set up a poll
        val pollMono1 = longPollingClient.consumeMessages("/api/long-polling/$channel")

        val stepVerifier1 = // Verify the first message
            StepVerifier.create(pollMono1)
                .expectNextMatches { response ->
                    (
                        response.containsKey("channel") && response["channel"] == channel &&
                            response["message"].toString()
                                .contains("First Long-Polling message")
                        ) ||
                        (
                            response.containsKey("message") &&
                                response["message"].toString()
                                    .contains("First Long-Polling message")
                            )
                }
                .thenCancel()
                .verifyLater()
        // Wait a bit to ensure the connection is established
        waitForConnection(1000)

        // Publish the first message
        publishMessage(channel, message1, contentType = MediaType.APPLICATION_JSON)

        // Then: Verify that the message was received via Long-Polling
        stepVerifier1.verify(Duration.ofSeconds(30))

        // For the second message
        // Set up another poll
        val pollMono2 = longPollingClient.consumeMessages("/api/long-polling/$channel")

        val stepVerifier2 = // Verify the second message
            StepVerifier.create(pollMono2)
                .expectNextMatches { response ->
                    (
                        response.containsKey("channel") && response["channel"] == channel &&
                            response["message"].toString()
                                .contains("Second Long-Polling message")
                        ) ||
                        (
                            response.containsKey("message") &&
                                response["message"].toString()
                                    .contains("Second Long-Polling message")
                            )
                }
                .thenCancel()
                .verifyLater()

        waitForConnection(1000)
        // Publish the second message
        publishMessage(channel, message2, contentType = MediaType.APPLICATION_JSON)

        stepVerifier2.verify(Duration.ofSeconds(30))

        // For the third message
        // Set up another poll
        val pollMono3 = longPollingClient.consumeMessages("/api/long-polling/$channel")

        val stepVerifier3 = // Verify the third message
            StepVerifier.create(pollMono3)
                .expectNextMatches { response ->
                    (
                        response.containsKey("channel") && response["channel"] == channel &&
                            response["message"].toString()
                                .contains("Third Long-Polling message")
                        ) ||
                        (
                            response.containsKey("message") &&
                                response["message"].toString()
                                    .contains("Third Long-Polling message")
                            )
                }
                .thenCancel()
                .verifyLater()

        waitForConnection(1000)
        // Publish the third message
        publishMessage(channel, message3, contentType = MediaType.APPLICATION_JSON)

        // Verify the third message
        stepVerifier3
            .verify(Duration.ofSeconds(30))
    }
}
