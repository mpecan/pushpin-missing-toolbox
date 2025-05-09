package io.github.mpecan.pmt.health

import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of PushpinHealthChecker.
 */
class DefaultPushpinHealthChecker(
    private val webClient: WebClient,
    private val healthCheckEnabled: Boolean,
    private val defaultTimeout: Long,
    private val pushpinService: PushpinService,
) : PushpinHealthChecker {
    private val logger = LoggerFactory.getLogger(DefaultPushpinHealthChecker::class.java)
    private val healthyServers = ConcurrentHashMap<String, PushpinServer>()

    /**
     * Checks the health of a single server.
     */
    override fun checkHealth(server: PushpinServer): Mono<Boolean> {
        return webClient.get()
            .uri(server.getHealthCheckUrl())
            .retrieve()
            .bodyToMono<String>()
            .doOnSuccess { response ->
                logger.debug("Health check response from server ${server.id}: $response")
            }
            .doOnError {
                logger.error("Error checking health of server ${server.id}: ${it.message}")
            }
            .map { true }
            .onErrorReturn(false)
            .timeout(Duration.ofMillis(defaultTimeout))
    }

    /**
     * Performs health checks on all servers.
     */
    @Scheduled(fixedDelayString = "\${pushpin.health-check-interval:60000}")
    override fun checkServerHealth(): Map<String, PushpinServer> {
        if (!healthCheckEnabled) {
            return pushpinService.getAllServers().associateBy { it.id }
        }

        val servers = pushpinService.getAllServers()
        servers.forEach { server ->
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

        return healthyServers
    }

    /**
     * Gets all healthy servers.
     */
    override fun getHealthyServers(): Map<String, PushpinServer> {
        return healthyServers
    }
}
