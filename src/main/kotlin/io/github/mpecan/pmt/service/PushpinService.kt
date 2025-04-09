package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
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
class PushpinService(private val pushpinProperties: PushpinProperties) {
    private val logger = LoggerFactory.getLogger(PushpinService::class.java)
    private val webClient = WebClient.builder().build()
    private val servers = ConcurrentHashMap<String, PushpinServer>()
    private val healthyServers = ConcurrentHashMap<String, PushpinServer>()
    private val counter = AtomicInteger(0)

    init {
        loadServers()
        if (pushpinProperties.healthCheckEnabled) {
            checkServerHealth()
        }
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
     * Performs health checks on all servers.
     */
    @Scheduled(fixedDelayString = "\${pushpin.health-check-interval:60000}")
    fun checkServerHealth() {
        if (!pushpinProperties.healthCheckEnabled) {
            return
        }

        servers.values.forEach { server ->
            checkHealth(server)
                .doOnSuccess { healthy ->
                    if (healthy) {
                        healthyServers[server.id] = server
                        logger.debug("Pushpin server ${server.id} is healthy")
                    } else {
                        healthyServers.remove(server.id)
                        logger.warn("Pushpin server ${server.id} is unhealthy")
                    }
                }
                .doOnError { error ->
                    healthyServers.remove(server.id)
                    logger.error("Error checking health of Pushpin server ${server.id}: ${error.message}")
                }
                .subscribe()
        }
    }

    /**
     * Checks the health of a single server.
     */
    private fun checkHealth(server: PushpinServer): Mono<Boolean> {
        return webClient.get()
            .uri(server.getHealthCheckUrl())
            .retrieve()
            .bodyToMono<String>()
            .doOnSuccess { response ->
                logger.debug("Health check response from server ${server.id}: $response")
            }
            .doOnError{
                logger.error("Error checking health of server ${server.id}: ${it.message}")
            }
            .map { true }
            .onErrorReturn(false)
            .timeout(Duration.ofMillis(pushpinProperties.defaultTimeout))
    }

    /**
     * Gets a server using round-robin load balancing.
     */
    private fun getServer(): PushpinServer? {
        if (healthyServers.isEmpty()) {
            return servers.values.firstOrNull()
        }

        val serverList = healthyServers.values.toList()
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
     * Gets all healthy servers.
     */
    fun getHealthyServers(): List<PushpinServer> {
        return healthyServers.values.toList()
    }

    /**
     * Gets a server by ID.
     */
    fun getServerById(id: String): PushpinServer? {
        return servers[id]
    }
}

private fun Message.toPushPin()= PushpinMessage(
        id = UUID.randomUUID().toString(),
        channel = this.channel,
        formats = listOf(
            "websocket" to PushpinFormat(
                content = this.data.toString(),
                action = "send"
            ),
            "http-stream" to PushpinFormat(
                content = "data:"+this.data.toString()+"\n\n",
                action = "send",
            ),
            "http-response" to PushpinFormat(
                body = this.data.toString()
            )
        ).toMap()
    )
