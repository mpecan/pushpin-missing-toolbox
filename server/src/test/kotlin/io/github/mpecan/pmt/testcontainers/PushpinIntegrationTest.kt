package io.github.mpecan.pmt.testcontainers

import io.github.mpecan.pmt.config.PushpinProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for integration tests that require a Pushpin server.
 *
 * This class provides common setup and utility methods for all Pushpin integration tests.
 * It handles container creation, property configuration, and common test operations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@AutoConfigureWebTestClient
abstract class PushpinIntegrationTest {
    @LocalServerPort
    protected var port: Int = 0

    protected val webClient = WebClient.builder().build()

    @Autowired
    protected lateinit var pushpinProperties: PushpinProperties

    /**
     * Publishes a message to the specified channel.
     *
     * @param channel The channel to publish to
     * @param message The message to publish (can be a string or a map)
     * @param eventType Optional event type for SSE
     * @return True if the message was published successfully
     */
    protected fun publishMessage(
        channel: String,
        message: Any,
        eventType: String? = null,
        contentType: MediaType = MediaType.TEXT_PLAIN,
    ): Boolean {
        val uri =
            if (eventType != null) {
                "http://localhost:$port/api/pushpin/publish/$channel?event=$eventType"
            } else {
                "http://localhost:$port/api/pushpin/publish/$channel"
            }

        val response =
            webClient
                .post()
                .uri(uri)
                .contentType(contentType)
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block()

        return response?.statusCode?.is2xxSuccessful ?: false
    }

    /**
     * Publishes a message with specific transport types.
     *
     * @param channel The channel to publish to
     * @param data The message data
     * @param transports List of transport types to use
     * @param eventType Optional event type for SSE
     * @return True if the message was published successfully
     */
    protected fun publishMessageWithTransports(
        channel: String,
        data: Any,
        transports: List<Any>,
        eventType: String? = null,
    ): Boolean {
        val message =
            mapOf(
                "channel" to channel,
                "data" to data,
                "transports" to transports,
            )

        val uri =
            if (eventType != null) {
                "http://localhost:$port/api/pushpin/publish?event=$eventType"
            } else {
                "http://localhost:$port/api/pushpin/publish"
            }

        val response =
            webClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block()

        return response?.statusCode?.is2xxSuccessful ?: false
    }

    /**
     * Waits for the connection to be established.
     * This is a common pattern in the tests to ensure the client is connected before publishing messages.
     *
     * @param milliseconds The time to wait in milliseconds
     */
    protected fun waitForConnection(milliseconds: Long = 1000) {
        Thread.sleep(milliseconds)
    }
}
