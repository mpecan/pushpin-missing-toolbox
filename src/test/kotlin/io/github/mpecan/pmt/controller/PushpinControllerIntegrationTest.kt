package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.testcontainers.PushpinIntegrationTest
import io.github.mpecan.pmt.testcontainers.TestcontainersUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.junit.jupiter.Container
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*


/**
 * Integration tests for PushpinController.
 *
 * These tests verify that the PushpinController can correctly handle HTTP requests
 * and interact with the PushpinService.
 */
@AutoConfigureWebTestClient
class PushpinControllerIntegrationTest : PushpinIntegrationTest() {

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

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var pushpinProperties: PushpinProperties

    @Test
    fun `should get all servers`() {
        // When/Then
        webTestClient.get()
            .uri("/api/pushpin/servers")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo("pushpin-test")
            .jsonPath("$[0].host").isEqualTo("localhost")
    }

    @Test
    fun `should get server by id`() {
        // When/Then
        webTestClient.get()
            .uri("/api/pushpin/servers/pushpin-test")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo("pushpin-test")
            .jsonPath("$.host").isEqualTo("localhost")
    }

    @Test
    fun `should return 404 for non-existent server`() {
        // When/Then
        webTestClient.get()
            .uri("/api/pushpin/servers/non-existent")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should publish message`() {
        // Given
        val message = Message.simple(
            "test-channel-${UUID.randomUUID()}",
            mapOf("text" to "Hello from integration test!")
        )

        // When/Then
        webTestClient.post()
            .uri("/api/pushpin/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Message published successfully")
    }

    @Test
    fun `should publish message to specific channel`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val data = mapOf("text" to "Hello from integration test!")

        // When/Then
        webTestClient.post()
            .uri("/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(data)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Message published successfully")
    }

    @Test
    fun `should publish message with event type`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val event = "test-event"
        val data = mapOf("text" to "Hello from integration test!")

        // When/Then
        webTestClient.post()
            .uri("/api/pushpin/publish/$channel?event=$event")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(data)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Message published successfully")
    }

    @Test
    fun `should receive message via SSE`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "\"Hello from SSE test!\""

        // Create an SSE client that connects to the events endpoint through Pushpin
        // We need to connect to the Spring Boot application, which will be proxied through Pushpin

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
                ServerSentEvent.builder(messageText.replace("\"","")).build()
            )
            .thenCancel()
            .verifyLater()

        // Wait a bit to ensure the connection is established
        Thread.sleep(1000)

        // When: Publish a message to the channel
        webTestClient.post()
            .uri("/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(messageText)
            .exchange()
            .expectStatus().isOk

        // Then: Verify that the message was received via SSE
        stepVerifier.verify(Duration.ofSeconds(10))
    }
}


class SseClient(baseUrl: String) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun consumeEvents(endpoint: String): Flux<ServerSentEvent<String>> {

        val flux = webClient.get()
            .uri(endpoint)
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .doOnError { t -> println(t.message) }
        return flux.timeout(Duration.ofSeconds(10))
    }
}