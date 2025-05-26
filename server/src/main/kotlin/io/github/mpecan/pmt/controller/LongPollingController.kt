package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.grip.GripApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for HTTP Long-Polling.
 * 
 * This controller handles HTTP Long-Polling connections and implements
 * the GRIP protocol for subscribing to channels.
 */
@RestController
@RequestMapping("/api/long-polling")
class LongPollingController {

    /**
     * Subscribes to a channel using the GRIP protocol with long-polling.
     * 
     * This endpoint keeps the connection open for a specified timeout period
     * and adds the necessary GRIP headers to tell Pushpin to hold the connection
     * and subscribe to the specified channel.
     * 
     * @param channel The channel to subscribe to
     * @return A Mono that completes after the timeout, with GRIP headers
     */
    @GetMapping("/{channel}")
    fun subscribe(@PathVariable channel: String): ResponseEntity<Mono<Map<String, Any>>> {
        // Create a response that will be sent after the timeout
        val response: Mono<Map<String, Any>> = Mono.just(mapOf(
            "success" to true,
            "message" to "No messages received within timeout period",
            "channel" to channel
        ))

        // Return the response with GRIP headers using the new API
        return GripApi.longPollingResponse<Mono<Map<String, Any>>>(channel, timeout = 20)
            .body(response)
    }
}
