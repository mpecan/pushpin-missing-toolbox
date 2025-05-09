package io.github.mpecan.pmt.client

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Client for consuming HTTP Long-Polling responses.
 */
class LongPollingClient(baseUrl: String) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    /**
     * Consumes messages from a long-polling endpoint.
     *
     * This method repeatedly polls the endpoint, creating a Flux of responses.
     * Each poll waits for a response (which will come either when a message is available
     * or when the timeout is reached), then immediately polls again.
     *
     * @param endpoint The endpoint to poll
     * @param pollCount The number of times to poll (default: 3)
     * @return A Flux of response objects
     */
    fun consumeMessages(endpoint: String, pollCount: Int = 3): Flux<Map<String, Any>> {
        // Create a function that performs a single poll
        val pollOnce: () -> Mono<Map<String, Any>> = {
            webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Map::class.java)
                .map { it as Map<String, Any> }
                .doOnNext { println("Received long-polling response: $it") }
                .timeout(Duration.ofSeconds(30))
        }

        // Create a Flux that repeatedly polls
        return Flux.range(1, pollCount)
            .delayElements(Duration.ofMillis(500))
            .concatMap { pollOnce() }
            .doOnError { t -> println("Error in long-polling: ${t.message}") }
    }
}
