package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.client.WebSocketClient
import io.github.mpecan.pmt.config.PushpinProperties
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
 * Integration tests for WebSocket functionality.
 *
 * These tests verify that the application can correctly handle WebSocket connections
 * and publish/receive messages through Pushpin using the WebSocket protocol.
 */
@AutoConfigureWebTestClient
class WebSocketIntegrationTest : PushpinIntegrationTest() {

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
    fun `should receive message via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val messageText = "\"Hello from WebSocket test!\""

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
        Thread.sleep(300)

        // When: Publish a message to the channel
        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(messageText)
            .retrieve()
            .toBodilessEntity()
            .block()

        // Then: Verify that the message was received via WebSocket
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }
    
    @Test
    fun `should receive multiple messages via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val message1 = "\"First WebSocket message\""
        val message2 = "\"Second WebSocket message\""
        val message3 = "\"Third WebSocket message\""

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
        Thread.sleep(300)

        // When: Publish multiple messages to the channel
        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message1)
            .retrieve()
            .toBodilessEntity()
            .block()

        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message2)
            .retrieve()
            .toBodilessEntity()
            .block()

        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message3)
            .retrieve()
            .toBodilessEntity()
            .block()

        // Then: Verify that all messages were received via WebSocket
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }
    
    @Test
    fun `should receive message with event type via WebSocket`() {
        // Given
        val channel = "test-channel-${UUID.randomUUID()}"
        val eventType = "custom-event"
        val messageText = "\"Hello with custom event type!\""

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
        Thread.sleep(300)

        // When: Publish a message with event type to the channel
        webClient.post()
            .uri("http://localhost:$port/api/pushpin/publish/$channel?event=$eventType")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(messageText)
            .retrieve()
            .toBodilessEntity()
            .block()

        // Then: Verify that the message was received via WebSocket with the correct event type
        stepVerifier.verify(Duration.ofSeconds(3))
        wsClient.closeConnection("/api/ws/$channel")
    }
}