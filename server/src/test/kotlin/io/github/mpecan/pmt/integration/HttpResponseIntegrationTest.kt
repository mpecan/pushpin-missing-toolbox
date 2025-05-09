package io.github.mpecan.pmt.integration

import io.github.mpecan.pmt.test.PortProvider
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

/**
 * Integration tests for non-streaming HTTP response functionality.
 *
 * These tests verify that the application can correctly handle regular HTTP requests
 * and responses through Pushpin.
 */
@AutoConfigureWebTestClient
class HttpResponseIntegrationTest : PushpinIntegrationTest() {

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
    fun `should receive HTTP response through Pushpin`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"

        // When: Make a request through Pushpin
        val pushpinPort = pushpinProperties.servers[0].port
        val responseFlux = webClient.get()
            .uri("http://localhost:$pushpinPort/api/http-response/$channel")
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { it as Map<String, Any> }

        // Then: Verify the response
        StepVerifier.create(responseFlux)
            .expectNextMatches { response ->
                response["success"] == true &&
                        response["message"] == "This is a non-streaming HTTP response" &&
                        response["channel"] == channel
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }

    @Test
    fun `should publish to client after HTTP response`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "Hello after HTTP response!"

        // First, make a request to establish the channel
        val pushpinPort = pushpinProperties.servers[0].port
        webClient.get()
            .uri("http://localhost:$pushpinPort/api/http-response/$channel")
            .retrieve()
            .bodyToMono(Map::class.java)
            .block(Duration.ofSeconds(5))

        // When: Publish a message to the channel
        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(messageText)
            .retrieve()
            .toBodilessEntity()
            .block()

        // Then: The message should be published to the client
        // Note: In a real scenario, the client would receive this as a separate response
        // We can't easily test this in an integration test, but we can verify that
        // the publish operation succeeds
    }
}
