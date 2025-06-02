package io.github.mpecan.pmt.transport.http

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.model.PushpinHttpMessage
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.transport.PushpinTransport
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * HTTP transport implementation for publishing messages to Pushpin servers.
 */
class HttpTransport(
    private val webClient: WebClient,
    private val messageSerializer: MessageSerializer,
    private val discoveryManager: PushpinDiscoveryManager? = null,
    private val defaultTimeout: Long = 5000L,
) : PushpinTransport {
    private val logger = LoggerFactory.getLogger(HttpTransport::class.java)

    /**
     * Publishes a message to a single Pushpin server via HTTP.
     */
    private fun publishToServer(
        server: PushpinServer,
        message: Message,
    ): Mono<Boolean> {
        logger.info("Publishing message to server: ${server.id} via HTTP")
        val pushpinMessage = messageSerializer.serialize(message)
        val httpMessage = PushpinHttpMessage(listOf(pushpinMessage))

        return webClient
            .post()
            .uri("${server.getHttpUrl()}/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(httpMessage)
            .retrieve()
            .onStatus(
                { status ->
                    logger.info(
                        "Publishing message to server: ${server.id} at ${server.getBaseUrl()} - status: $status",
                    )
                    status.isError
                },
                { clientResponse ->
                    logger.error("Failed to publish message to server: ${server.id} at ${server.getBaseUrl()}")
                    Mono.error(RuntimeException("Failed to publish message: ${clientResponse.statusCode()}"))
                },
            ).bodyToMono<String>()
            .doOnSuccess {
                logger.info("Message published to Pushpin server ${server.id}: $it")
            }.map { true }
            .onErrorResume { error ->
                logger.error("Error publishing message to Pushpin server ${server.id}: ${error.message}")
                Mono.just(false)
            }.timeout(Duration.ofMillis(defaultTimeout))
    }

    /**
     * Publishes a message to the specified servers.
     * This method is provided for backward compatibility and testing.
     */
    fun publishMessage(
        servers: List<PushpinServer>,
        message: Message,
    ): Mono<Boolean> {
        if (servers.isEmpty()) {
            logger.warn("No servers to publish to")
            return Mono.just(false)
        }

        return Flux
            .merge(
                servers.map { server ->
                    publishToServer(server, message)
                },
            ).reduce { acc, value ->
                acc && value
            }
    }

    /**
     * Implements the PushpinTransport interface method.
     * Publishes a message to all active Pushpin servers.
     */
    override fun publish(message: Message): Mono<Boolean> {
        val servers = discoveryManager?.getAllServers() ?: emptyList()
        return publishMessage(servers, message)
    }
}
