package io.github.mpecan.pmt.transport.http

import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.transport.health.TransportHealthChecker
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * HTTP-based health checker for Pushpin servers.
 * * Performs health checks by making HTTP GET requests to the server's
 * health check endpoint (typically /api/health/check).
 */
class HttpHealthChecker(
    private val webClient: WebClient,
    private val defaultTimeout: Long = 5000L,
) : TransportHealthChecker {
    private val logger = LoggerFactory.getLogger(HttpHealthChecker::class.java)

    /**
     * Checks the health of a single server via HTTP.
     */
    override fun checkHealth(server: PushpinServer): Mono<Boolean> {
        return webClient.get()
            .uri(server.getHealthCheckUrl())
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { response ->
                logger.debug("Health check response from server ${server.id}: $response")
            }
            .doOnError {
                logger.error("Error checking health of server ${server.id}: ${it.message}")
            }
            .timeout(Duration.ofMillis(defaultTimeout))
            .map { true }
            .onErrorReturn(false)
    }

    /**
     * Returns the transport type for this health checker.
     */
    override fun getTransportType(): String = "http"
}
