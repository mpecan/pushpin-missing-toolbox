package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.grip.GripApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Controller for HTTP Streaming (non-SSE).
 * 
 * This controller handles HTTP Streaming connections and implements
 * the GRIP protocol for subscribing to channels using plain HTTP streaming
 * rather than Server-Sent Events.
 */
@RestController
@RequestMapping("/api/http-stream")
class HttpStreamingController {

    /**
     * Subscribes to a channel using the GRIP protocol with HTTP streaming.
     * 
     * This endpoint keeps the connection open and adds the necessary GRIP headers
     * to tell Pushpin to hold the connection and subscribe to the specified channel.
     * Unlike SSE, this uses plain HTTP streaming without the SSE format.
     * 
     * @param channel The channel to subscribe to
     * @return A Flux that never completes, with GRIP headers
     */
    @GetMapping("/{channel}")
    fun subscribe(@PathVariable channel: String): ResponseEntity<Flux<String>> {
        // Create a Flux that never completes to keep the connection open
        val flux = Flux.just<String>("Successfully subscribed to channel: $channel\n")
        
        // Return the response with GRIP headers using the new API
        return GripApi.streamingResponse(channel)
            .body(flux)
    }
}