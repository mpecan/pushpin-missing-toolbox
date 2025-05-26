package io.github.mpecan.pmt.client

import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.net.URI

class WebSocketClient(private val baseUrl: String) {
    private val client = ReactorNettyWebSocketClient()
    private val activeSubscriptions = mutableMapOf<String, Any>()

    fun consumeMessages(endpoint: String): Flux<String> {
        // Use a unicast sink that doesn't buffer
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        val uri = URI.create("$baseUrl$endpoint")

        println("Creating WebSocket connection to $uri")

        // This subscription needs to be kept alive
        val subscription = client.execute(uri) { session ->
            println("WebSocket session established to $uri")

            session.receive()
                .doOnNext {
                    val message = it.payloadAsText
                    println("WebSocketClient - Received message from $uri: '$message'")

                    // Always emit the message, even if it's empty
                    val result = sink.tryEmitNext(message)
                    println("WebSocketClient - Emit result for $uri: $result")
                }
                .doOnComplete {
                    println("Connection closed normally: $uri")
                    sink.tryEmitComplete()
                    activeSubscriptions.remove(endpoint)
                }
                .doOnError { error ->
                    println("Connection error for $uri: ${error.message}")
                    sink.tryEmitError(error)
                    activeSubscriptions.remove(endpoint)
                }
                .doOnCancel {
                    println("Connection cancelled for $uri")
                    activeSubscriptions.remove(endpoint)
                }
                .then()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* onNext - success */ println("Connection successfully established") },
                { error -> /* onError */ println("Failed to establish connection: ${error.message}") },
                { /* onComplete */ println("Connection setup completed") },
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

    fun closeAllConnections() {
        activeSubscriptions.clear()
        println("Closed all WebSocket connections")
    }
}
