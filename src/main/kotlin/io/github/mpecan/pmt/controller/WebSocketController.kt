package io.github.mpecan.pmt.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/ws")
class WebSocketController {

    // return a response with the correct result for websockets c:{"type": "subscribe", "channel": "mychannel"}
    @GetMapping("/{channel}")
    fun subscribe(@PathVariable channel: String): ResponseEntity<Flux<String>> {
        // Create a Flux that never completes to keep the connection open
        val flux = Flux.just("c:{\"type\": \"subscribe\", \"channel\": \"$channel\"}")

        // Return the response with GRIP headers
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Grip-Hold", "websocket")
            .header("Grip-Channel", channel)
            .body(flux)
    }
}