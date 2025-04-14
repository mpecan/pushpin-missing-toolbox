package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.health.PushpinHealthChecker
import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * Controller for Pushpin-related operations.
 */
@RestController
@RequestMapping("/api/pushpin")
class PushpinController(
    private val pushpinService: PushpinService,
    private val pushpinHealthChecker: PushpinHealthChecker
) {

    /**
     * Publishes a message to a channel.
     */
    @PostMapping("/publish")
    fun publishMessage(@RequestBody message: Message): Mono<ResponseEntity<Map<String, Any>>> {
        println("publishing message: $message")
        return pushpinService.publishMessage(message)
            .map { success ->
                if (success) {
                    ResponseEntity.ok(mapOf(
                        "success" to true,
                        "message" to "Message published successfully",
                        "timestamp" to Date()
                    ))
                } else {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                        "success" to false,
                        "message" to "Failed to publish message",
                        "timestamp" to Date()
                    ))
                }
            }
    }

    /**
     * Publishes a message to a specific channel.
     */
    @PostMapping("/publish/{channel}", consumes = ["application/json"])
    fun publishToChannel(
        @PathVariable channel: String,
        @RequestParam(required = false) event: String?,
        @RequestBody data: Any
    ): Mono<ResponseEntity<Map<String, Any>>> {
        val message = if (event != null) {
            Message.event(channel, event, data)
        } else {
            Message.simple(channel, data)
        }

        return publishMessage(message)
    }

    @PostMapping("/publish/{channel}", consumes = ["text/plain"])
    fun publishToChannelText(
        @PathVariable channel: String,
        @RequestParam(required = false) event: String?,
        @RequestBody data: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return publishToChannel(channel, event, data)
    }

    /**
     * Gets all configured Pushpin servers.
     */
    @GetMapping("/servers")
    fun getAllServers(): ResponseEntity<List<PushpinServer>> {
        return ResponseEntity.ok(pushpinService.getAllServers())
    }

    /**
     * Gets all healthy Pushpin servers.
     */
    @GetMapping("/servers/healthy")
    fun getHealthyServers(): ResponseEntity<List<PushpinServer>> {
        return ResponseEntity.ok(pushpinHealthChecker.getHealthyServers().values.toList())
    }

    /**
     * Gets a Pushpin server by ID.
     */
    @GetMapping("/servers/{id}")
    fun getServerById(@PathVariable id: String): ResponseEntity<PushpinServer> {
        val server = pushpinService.getServerById(id)
        return if (server != null) {
            ResponseEntity.ok(server)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
