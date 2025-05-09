package io.github.mpecan.pmt.client

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * Client for consuming HTTP Streaming responses.
 */
class HttpStreamingClient(baseUrl: String) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    /**
     * Consumes a stream of text from an HTTP streaming endpoint.
     * * @param endpoint The endpoint to stream from
     * @return A Flux of text chunks
     */
    fun consumeStream(endpoint: String): Flux<String> {
        return webClient.get()
            .uri(endpoint)
            .retrieve()
            .bodyToFlux(String::class.java)
            .doOnNext { println("Received HTTP stream chunk: $it") }
            .doOnError { t -> println("Error in HTTP streaming: ${t.message}") }
            .timeout(Duration.ofSeconds(10))
    }
}
