package io.github.mpecan.pmt.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Controller for Server-Sent Events (SSE) endpoints.
 */
@RestController
@RequestMapping("/api/events")
class EventStreamController {
    private val emitters = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()

    /**
     * Subscribes to a channel.
     */
    @GetMapping("/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(@PathVariable channel: String): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)
        
        // Add the emitter to the channel's list
        emitters.computeIfAbsent(channel) { CopyOnWriteArrayList() }.add(emitter)
        
        // Remove the emitter when it completes or times out
        emitter.onCompletion { removeEmitter(channel, emitter) }
        emitter.onTimeout { removeEmitter(channel, emitter) }
        emitter.onError { removeEmitter(channel, emitter) }
        
        // Send a connection established event
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data(mapOf("channel" to channel, "status" to "connected"))
            )
        } catch (e: IOException) {
            removeEmitter(channel, emitter)
        }
        
        return emitter
    }

    /**
     * Publishes a message to a channel.
     */
    @PostMapping("/{channel}")
    fun publish(@PathVariable channel: String, @RequestBody message: Any): ResponseEntity<Map<String, Any>> {
        val channelEmitters = emitters[channel]
        
        if (channelEmitters == null || channelEmitters.isEmpty()) {
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "No subscribers for channel: $channel"
            ))
        }
        
        val deadEmitters = mutableListOf<SseEmitter>()
        
        // Send the message to all emitters for the channel
        channelEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .data(message)
                )
            } catch (e: IOException) {
                deadEmitters.add(emitter)
            }
        }
        
        // Remove dead emitters
        deadEmitters.forEach { emitter ->
            removeEmitter(channel, emitter)
        }
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Message published to ${channelEmitters.size - deadEmitters.size} subscribers"
        ))
    }

    /**
     * Publishes a message with an event type to a channel.
     */
    @PostMapping("/{channel}/{event}")
    fun publishEvent(
        @PathVariable channel: String,
        @PathVariable event: String,
        @RequestBody message: Any
    ): ResponseEntity<Map<String, Any>> {
        val channelEmitters = emitters[channel]
        
        if (channelEmitters == null || channelEmitters.isEmpty()) {
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "No subscribers for channel: $channel"
            ))
        }
        
        val deadEmitters = mutableListOf<SseEmitter>()
        
        // Send the message to all emitters for the channel
        channelEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(event)
                        .data(message)
                )
            } catch (e: IOException) {
                deadEmitters.add(emitter)
            }
        }
        
        // Remove dead emitters
        deadEmitters.forEach { emitter ->
            removeEmitter(channel, emitter)
        }
        
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Event published to ${channelEmitters.size - deadEmitters.size} subscribers"
        ))
    }

    /**
     * Removes an emitter from a channel.
     */
    private fun removeEmitter(channel: String, emitter: SseEmitter) {
        val channelEmitters = emitters[channel]
        channelEmitters?.remove(emitter)
        
        // Remove the channel if there are no more emitters
        if (channelEmitters?.isEmpty() == true) {
            emitters.remove(channel)
        }
    }

    /**
     * Gets the number of subscribers for a channel.
     */
    @GetMapping("/{channel}/subscribers")
    fun getSubscribers(@PathVariable channel: String): ResponseEntity<Map<String, Any>> {
        val count = emitters[channel]?.size ?: 0
        
        return ResponseEntity.ok(mapOf(
            "channel" to channel,
            "subscribers" to count
        ))
    }

    /**
     * Gets all channels with subscribers.
     */
    @GetMapping("/channels")
    fun getChannels(): ResponseEntity<Map<String, Any>> {
        val channels = emitters.keys.toList()
        
        return ResponseEntity.ok(mapOf(
            "channels" to channels,
            "count" to channels.size
        ))
    }
}