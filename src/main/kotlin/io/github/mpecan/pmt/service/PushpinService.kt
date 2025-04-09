package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.formatter.HttpResponseMessageFormatter
import io.github.mpecan.pmt.formatter.HttpStreamMessageFormatter
import io.github.mpecan.pmt.formatter.WebSocketMessageFormatter
import io.github.mpecan.pmt.model.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service for managing Pushpin servers and publishing messages.
 */
@Service
class PushpinService(
    private val pushpinProperties: PushpinProperties,
    private val webSocketFormatter: WebSocketMessageFormatter,
    private val httpStreamFormatter: HttpStreamMessageFormatter,
    private val httpResponseFormatter: HttpResponseMessageFormatter
) {
    private val logger = LoggerFactory.getLogger(PushpinService::class.java)
    private val webClient = WebClient.builder().build()
    private val servers = ConcurrentHashMap<String, PushpinServer>()
    private val counter = AtomicInteger(0)

    init {
        loadServers()
    }

    /**
     * Loads server configurations from properties.
     */
    private fun loadServers() {
        pushpinProperties.servers.forEach { serverProps ->
            val server = serverProps.toPushpinServer()
            if (server.active) {
                servers[server.id] = server
                logger.info("Loaded Pushpin server: ${server.id} at ${server.getBaseUrl()}")
            }
        }

        if (servers.isEmpty()) {
            logger.warn("No active Pushpin servers configured")
        }
    }

    /**
     * Gets a server using round-robin load balancing.
     */
    private fun getServer(): PushpinServer? {
        // We no longer check health here, just return a server using round-robin
        val serverList = servers.values.toList()
        if (serverList.isEmpty()) {
            return null
        }

        val index = counter.getAndIncrement() % serverList.size
        return serverList[index]
    }

    /**
     * Publishes a message to a Pushpin server.
     */
    fun publishMessage(message: Message): Mono<Boolean> {
        val server = getServer() ?: return Mono.error(IllegalStateException("No Pushpin servers available"))
        logger.info("Publishing message to server: ${server.id}")
        val httpMessage = PushpinHttpMessage(listOf(message.toPushPin()))
        return webClient.post()
            .uri("${server.getControlUrl()}/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(httpMessage)
            .retrieve()
            .onStatus(
                { status ->
                    logger.info("Publishing message to server: ${server.id} at ${server.getBaseUrl()} - status: $status")
                    status.isError
                },
                { clientResponse ->
                    logger.error("Failed to publish message to server: ${server.id} at ${server.getBaseUrl()}")
                    Mono.error(RuntimeException("Failed to publish message: ${clientResponse.statusCode()}"))
                }
            )
            .bodyToMono<String>()
            .doOnSuccess{
                logger.info("Message published to Pushpin server ${server.id}: $it")
            }
            .map {
                true
            }
            .onErrorResume { error ->
                logger.error("Error publishing message to Pushpin server ${server.id}: ${error.message}")
                Mono.just(false)
            }
            .timeout(Duration.ofMillis(pushpinProperties.defaultTimeout))
    }

    /**
     * Gets all configured servers.
     */
    fun getAllServers(): List<PushpinServer> {
        return servers.values.toList()
    }


    /**
     * Gets a server by ID.
     */
    fun getServerById(id: String): PushpinServer? {
        return servers[id]
    }

    /**
     * Converts a Message to a PushpinMessage using the configured formatters.
     */
    private fun Message.toPushPin() = PushpinMessage(
        channel = this.channel,
        formats = mapOf(
            "ws-message" to webSocketFormatter.format(this),
            "http-stream" to httpStreamFormatter.format(this),
            "http-response" to httpResponseFormatter.format(this)
        )
    )
}
