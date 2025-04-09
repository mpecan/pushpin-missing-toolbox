package io.github.mpecan.pmt.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * Client for consuming Server-Sent Events.
 */
class SseClient(baseUrl: String) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun consumeEvents(endpoint: String): Flux<ServerSentEvent<String>> {
        val flux = webClient.get()
            .uri(endpoint)
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .doOnError { t -> println(t.message) }
        return flux.timeout(Duration.ofSeconds(10))
    }
}