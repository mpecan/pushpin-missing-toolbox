package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.EncryptionService
import io.github.mpecan.pmt.transport.PushpinTransport
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service for managing Pushpin servers and publishing messages.
 */
@Service
class PushpinService(
    private val discoveryManager: PushpinDiscoveryManager,
    private val encryptionService: EncryptionService,
    private val auditService: AuditService,
    private val metricsService: MetricsService,
    pushpinTransports: List<PushpinTransport>,
) {
    private val transport =
        pushpinTransports
            .also {
                check(
                    it.isNotEmpty(),
                ) { "No Pushpin transport implementations found. Ensure you have the correct dependencies." }
                check(
                    it.size == 1,
                ) { "Multiple Pushpin transport implementations found. Please ensure only one is configured." }
            }.first()

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
        val timer = metricsService.startTimer()

        // Log publishing activity for audit purposes
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null) {
            auditService.logChannelAccess(
                authentication.name,
                "backend-service",
                message.channel,
                "publish message",
            )
        }

        // Determine transport type
        val transportType =
            transport.javaClass.simpleName
                .lowercase()
                .replace("pushpintransport", "")
                .ifEmpty { "unknown" }

        // Get server info for metrics
        val servers = discoveryManager.getAllServers()
        val server = servers.firstOrNull { it.active }?.id ?: "unknown"

        // Encrypt message data if needed
        val messageToPublish =
            if (encryptionService.isEncryptionEnabled()) {
                // Convert data to string, encrypt it, and create a new message
                val dataStr = message.data.toString()
                val encryptedData = encryptionService.encrypt(dataStr)
                message.copy(data = encryptedData)
            } else {
                message
            }

        return transport
            .publish(messageToPublish)
            .doOnSuccess { success ->
                // Stop timer
                metricsService.stopTimer(timer, "publish", server)

                if (success) {
                    // Record successful message
                    metricsService.recordMessageSent(server, transportType, "success")

                    // Record throughput
                    val messageSize =
                        messageToPublish.data
                            .toString()
                            .toByteArray()
                            .size
                            .toLong()
                    metricsService.recordThroughput(transportType, messageSize)
                } else {
                    // Record failed message
                    metricsService.recordMessageSent(server, transportType, "failed")
                    metricsService.recordPublishError(server, "publish_failed")
                }
            }.doOnError { error ->
                // Stop timer
                metricsService.stopTimer(timer, "publish", server)

                // Record error
                metricsService.recordMessageError(server, transportType, error.javaClass.simpleName)
                metricsService.recordPublishError(server, error.javaClass.simpleName)
            }
    }

    /**
     * Gets all configured servers.
     */
    fun getAllServers(): List<PushpinServer> = discoveryManager.getAllServers()

    /**
     * Gets a server by ID.
     */
    fun getServerById(id: String): PushpinServer? = discoveryManager.getServerById(id)
}
