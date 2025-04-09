package io.github.mpecan.pmt.client

import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class WebSocketClient(private val baseUrl: String) {
    private val client = ReactorNettyWebSocketClient()
    private val activeSubscriptions = mutableMapOf<String, Any>()

    fun consumeMessages(endpoint: String): Flux<String> {
        val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
        val uri = URI.create("$baseUrl$endpoint")

        // This subscription needs to be kept alive
        val subscription = client.execute(uri) { session ->
            println("WebSocket session established to $uri")

            session.receive()
                .doOnNext {
                    println("Received message: ${it.payloadAsText}")
                    sink.tryEmitNext(it.payloadAsText)
                }
                .doOnComplete {
                    println("Connection closed normally")
                    sink.tryEmitComplete()
                    activeSubscriptions.remove(endpoint)
                }
                .doOnError { error ->
                    println("Connection error: ${error.message}")
                    sink.tryEmitError(error)
                    activeSubscriptions.remove(endpoint)
                }
                .doOnCancel {
                    println("Connection cancelled")
                    activeSubscriptions.remove(endpoint)
                }
                .then()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* onNext - success */ println("Connection successfully established") },
                { error -> /* onError */ println("Failed to establish connection: ${error.message}") },
                { /* onComplete */ println("Connection setup completed") }
            )

        // Store the subscription to prevent it from being garbage collected
        activeSubscriptions[endpoint] = subscription

        // Return the flux that will emit messages
        return sink.asFlux()
    }

    fun closeConnection(endpoint: String) {
        activeSubscriptions.remove(endpoint)
        println("Removed subscription for $endpoint")
    }
}