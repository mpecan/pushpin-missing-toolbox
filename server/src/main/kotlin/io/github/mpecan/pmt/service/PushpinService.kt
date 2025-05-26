package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.model.PushpinHttpMessage
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.encryption.ChannelEncryptionService
import io.github.mpecan.pmt.service.zmq.ZmqPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Service for managing Pushpin servers and publishing messages.
 */
@Service
class PushpinService(
    private val pushpinProperties: PushpinProperties,
    private val discoveryManager: PushpinDiscoveryManager,
    private val messageSerializer: MessageSerializer,
    private val zmqPublisher: ZmqPublisher,
    private val channelEncryptionService: ChannelEncryptionService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(PushpinService::class.java)
    private val webClient = WebClient.builder().build()

    /**
     * Gets a server using round-robin load balancing.
     */
    private fun getServer(): List<PushpinServer>? {
        // We no longer check health here, just return a server using round-robin
        val serverList = discoveryManager.getAllServers()
        if (serverList.isEmpty()) {
            return null
        }

        return serverList
    }

    /**
     * Gets all active servers.
     */
    private fun getActiveServers(): List<PushpinServer> {
        return discoveryManager.getAllServers().filter { it.active }
    }

    /**
     * Publishes a message to Pushpin servers.
     *
     * If ZMQ is enabled, it will publish to all active servers via ZMQ.
     * Otherwise, it will publish to a single server via HTTP using round-robin selection.
     *
     * Publishing is typically done by backend services and should be authenticated
     * through other mechanisms (e.g., HMAC signing, API keys, service-to-service auth).
     * End users subscribe to channels but don't publish directly.
     */
    fun publishMessage(message: Message): Mono<Boolean> {
        // Log publishing activity for audit purposes
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null) {
            auditLogService.logChannelAccess(
                authentication.name,
                "backend-service",
                message.channel,
                "publish message"
            )
        }

        // Encrypt message data if needed
        if (pushpinProperties.security.encryption.enabled) {
            // Convert data to string, encrypt it, and create a new message
            val dataStr = message.data.toString()
            val encryptedData = channelEncryptionService.encrypt(dataStr)
            val encryptedMessage = message.copy(
                data = encryptedData
            )

            if (pushpinProperties.zmqEnabled) {
                return publishViaZmq(encryptedMessage)
            } else {
                return publishViaHttp(encryptedMessage)
            }
        } else {
            if (pushpinProperties.zmqEnabled) {
                return publishViaZmq(message)
            } else {
                return publishViaHttp(message)
            }
        }
    }

    /**
     * Publishes a message to all active Pushpin servers via ZMQ.
     */
    private fun publishViaZmq(message: Message): Mono<Boolean> {
        val servers = getActiveServers()
        if (servers.isEmpty()) {
            return Mono.error(IllegalStateException("No active Pushpin servers available"))
        }

        logger.info("Publishing message to ${servers.size} servers via ZMQ")

        // List all server details for debugging at trace level
        if (logger.isTraceEnabled) {
            servers.forEach { server ->
                logger.trace("Server: ${server.id}, Active: ${server.active}, Publish URL: ${server.getPublishUrl()}")
            }
        }

        // Use the new publish method that handles both serialization and ZMQ formatting
        val futures = zmqPublisher.publish(servers, message)

        // Log basic info about the message being published
        logger.info("Publishing message to channel: ${message.channel} via ZMQ")

        // Create CompletableFuture that completes when all publishing attempts have completed
        val allFuturesDone = CompletableFuture.allOf(*futures.map {
            CompletableFuture.supplyAsync { it.get() }
        }.toTypedArray())

        // Create Mono from CompletableFuture
        return Mono.fromFuture(allFuturesDone.thenApply {
            // Check if at least one server was successful
            val successCount = futures.count { it.get() }
            val anySuccess = successCount > 0
            logger.info("Published message to $successCount out of ${servers.size} servers via ZMQ")

            // In tests, add a small delay to allow ZMQ messages to propagate through the system
            if (pushpinProperties.testMode) {
                logger.info("Test mode enabled - adding a small delay for ZMQ message propagation")
                Thread.sleep(500)  // Small delay to allow messages to propagate in test environment
            }

            anySuccess
        })
            .timeout(Duration.ofMillis(pushpinProperties.defaultTimeout))
            .onErrorResume { error ->
                logger.error("Error publishing message via ZMQ: ${error.message}", error)
                Mono.just(false)
            }
    }

    /**
     * Publishes a message to a single Pushpin server via HTTP (legacy method).
     */
    private fun publishViaHttp(message: Message): Mono<Boolean> {
        val servers =
            getServer() ?: return Mono.error(IllegalStateException("No Pushpin servers available"))
        return Flux.merge(servers.map { server ->

            logger.info("Publishing message to server: ${server.id} via HTTP")
            val pushpinMessage = messageSerializer.serialize(message)
            val httpMessage = PushpinHttpMessage(listOf(pushpinMessage))

            return@map webClient.post()
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
                .doOnSuccess {
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
        }).reduce{ acc, value ->
            acc && value
        }
    }

    /**
     * Gets all configured servers.
     */
    fun getAllServers(): List<PushpinServer> {
        return discoveryManager.getAllServers()
    }

    /**
     * Gets a server by ID.
     */
    fun getServerById(id: String): PushpinServer? {
        return discoveryManager.getServerById(id)
    }
}