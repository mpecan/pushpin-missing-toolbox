package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.grip.GripApi
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

/**
 * Controller for SSE events.
 * * This controller handles Server-Sent Events (SSE) connections and implements
 * the GRIP protocol for subscribing to channels.
 */
@RestController
@RequestMapping("/api/events")
class EventsController {

    /**
     * Subscribes to a channel using the GRIP protocol.
     * * This endpoint keeps the connection open and adds the necessary GRIP headers
     * to tell Pushpin to hold the connection and subscribe to the specified channel.
     * * @param channel The channel to subscribe to
     * @return A Flux that never completes, with GRIP headers
     */
    @GetMapping("/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(@PathVariable channel: String): ResponseEntity<Flux<String>> {
        // Create a Flux that never completes to keep the connection open
        val flux = Flux.just<String>("Successfully subscribed to channel: $channel")

        // Return the response with GRIP headers using the new API
        return GripApi.sseResponse(channel)
            .body(flux)
    }
}
