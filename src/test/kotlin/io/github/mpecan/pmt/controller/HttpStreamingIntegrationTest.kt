package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.client.HttpStreamingClient
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.Transport
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClient
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
@AutoConfigureWebTestClient
class HttpStreamingIntegrationTest : PushpinIntegrationTest() {

    companion object {
        val definedPort = Random().nextInt(9000, 12000)

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
        fun configureProperties(registry: DynamicPropertyRegistry) {
            TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
            registry.add("server.port") { definedPort }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    private val webClient = WebClient.builder().build()

    @Autowired
    private lateinit var pushpinProperties: PushpinProperties

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
            .expectNext(messageText.replace("\"", ""))
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        Thread.sleep(1000)

        // When: Publish a message to the channel
        postStreamingMessage(channel, messageText)

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
            .expectNext(message1.replace("\"", ""))
            .expectNext(message2.replace("\"", ""))
            .expectNext(message3.replace("\"", ""))
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        Thread.sleep(1000)

        // When: Publish multiple messages to the channel
        postStreamingMessage(channel, message1)

        postStreamingMessage(channel, message2)

        postStreamingMessage(channel, message3)

        // Then: Verify that all messages were received via HTTP Streaming
        stepVerifier.verify(Duration.ofSeconds(10))
    }

    fun postStreamingMessage(channel: String, message: String) {
        val message = mapOf(
            "channel" to channel,
            "data" to message,
            "transports" to listOf(Transport.HttpStream)
        )
        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}
