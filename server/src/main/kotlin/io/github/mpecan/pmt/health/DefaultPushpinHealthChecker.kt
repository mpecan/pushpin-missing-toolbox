package io.github.mpecan.pmt.health

import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import io.github.mpecan.pmt.transport.health.TransportHealthChecker
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Default implementation of PushpinHealthChecker.
 * * This implementation supports multiple transport health checkers and will
 * use the appropriate checker based on the configured transport type.
 */
class DefaultPushpinHealthChecker(
    private val transportHealthCheckers: List<TransportHealthChecker>,
    private val healthCheckEnabled: Boolean,
    private val pushpinService: PushpinService,
    private val metricsService: MetricsService,
    private val defaultTransportType: String = "http",
) : PushpinHealthChecker {
    private val logger = LoggerFactory.getLogger(DefaultPushpinHealthChecker::class.java)
    private val healthyServers = ConcurrentHashMap<String, PushpinServer>()
    private val healthCheckerMap: Map<String, TransportHealthChecker> =
        transportHealthCheckers.associateBy {
            it.getTransportType()
        }

    init {
        logger.info("Initialized health checker with transport types: ${healthCheckerMap.keys}")
    }

    /**
     * Checks the health of a single server using the appropriate transport health checker.
     */
    override fun checkHealth(server: PushpinServer): Mono<Boolean> {
        val healthChecker = getHealthChecker()
        val startTime = System.currentTimeMillis()

        return if (healthChecker != null) {
            healthChecker
                .checkHealth(server)
                .doOnSuccess { healthy ->
                    val responseTime = System.currentTimeMillis() - startTime
                    metricsService.recordServerResponseTime(
                        server.id,
                        server.healthCheckPath,
                        responseTime,
                        TimeUnit.MILLISECONDS,
                    )
                    metricsService.updateServerHealth(server.id, healthy)
                }.doOnError { error ->
                    val responseTime = System.currentTimeMillis() - startTime
                    metricsService.recordServerResponseTime(
                        server.id,
                        server.healthCheckPath,
                        responseTime,
                        TimeUnit.MILLISECONDS,
                    )
                    metricsService.updateServerHealth(server.id, false)
                    metricsService.recordMessageError(server.id, defaultTransportType, error.javaClass.simpleName)
                }
        } else {
            logger.warn("No health checker available for transport type: $defaultTransportType")
            metricsService.updateServerHealth(server.id, false)
            Mono.just(false)
        }
    }

    /**
     * Gets the appropriate health checker based on the configured transport type.
     */
    private fun getHealthChecker(): TransportHealthChecker? =
        healthCheckerMap[defaultTransportType] ?: run {
            logger.warn("No health checker found for transport type: $defaultTransportType")
            // Fallback to first available health checker
            transportHealthCheckers.firstOrNull()
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
                }.doOnError { error ->
                    healthyServers.remove(server.id)
                    logger.error("Error checking health of Pushpin server ${server.id}: ${error.message}")
                }.subscribe()
        }

        return healthyServers
    }

    /**
     * Gets all healthy servers.
     */
    override fun getHealthyServers(): Map<String, PushpinServer> = healthyServers
}
