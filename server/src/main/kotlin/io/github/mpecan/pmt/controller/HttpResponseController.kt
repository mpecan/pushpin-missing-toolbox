package io.github.mpecan.pmt.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for non-streaming HTTP responses.
 * * This controller handles regular HTTP requests and demonstrates
 * how Pushpin can be used for non-streaming HTTP responses.
 */
@RestController
@RequestMapping("/api/http-response")
class HttpResponseController {

    /**
     * Returns a simple HTTP response for a channel.
     * * This endpoint demonstrates how Pushpin can be used for regular HTTP responses.
     * It adds a Grip-Channel header to allow Pushpin to later publish to clients
     * that have made requests to this endpoint.
     * * @param channel The channel to associate with this response
     * @return A simple HTTP response with Grip-Channel header
     */
    @GetMapping("/{channel}")
    fun getResponse(@PathVariable channel: String): ResponseEntity<Map<String, Any>> {
        // Create a simple response
        val response = mapOf(
            "success" to true,
            "message" to "This is a non-streaming HTTP response",
            "channel" to channel,
        )

        // Return the response with Grip-Channel header
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header("Grip-Channel", channel) // Associate this response with the channel
            .body(response)
    }
}
