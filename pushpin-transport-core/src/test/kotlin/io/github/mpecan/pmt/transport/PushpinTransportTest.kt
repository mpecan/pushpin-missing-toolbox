package io.github.mpecan.pmt.transport

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.model.Transport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PushpinTransportTest {
    @Test
    fun `should be functional interface with single publish method`() {
        val transport =
            PushpinTransport { message ->
                Mono.just(true)
            }

        val message = Message.simple("test-channel", "test-data")
        val result = transport.publish(message)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should handle successful publish`() {
        val transport =
            PushpinTransport { message ->
                Mono.just(true)
            }

        val message = Message.event("notifications", "user-message", mapOf("text" to "Hello"))
        val result = transport.publish(message)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should handle failed publish`() {
        val transport =
            PushpinTransport { message ->
                Mono.just(false)
            }

        val message = Message.simple("test-channel", "test-data")
        val result = transport.publish(message)

        StepVerifier
            .create(result)
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `should handle publish error`() {
        val transport =
            PushpinTransport { message ->
                Mono.error<Boolean>(RuntimeException("Connection failed"))
            }

        val message = Message.simple("test-channel", "test-data")
        val result = transport.publish(message)

        StepVerifier
            .create(result)
            .expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `should work with different message types`() {
        val transport =
            PushpinTransport { message ->
                when (message.channel) {
                    "success-channel" -> Mono.just(true)
                    "failure-channel" -> Mono.just(false)
                    else -> Mono.error(IllegalArgumentException("Unknown channel"))
                }
            }

        // Test success case
        val successMessage = Message.simple("success-channel", "data")
        StepVerifier
            .create(transport.publish(successMessage))
            .expectNext(true)
            .verifyComplete()

        // Test failure case
        val failureMessage = Message.simple("failure-channel", "data")
        StepVerifier
            .create(transport.publish(failureMessage))
            .expectNext(false)
            .verifyComplete()

        // Test error case
        val errorMessage = Message.simple("unknown-channel", "data")
        StepVerifier
            .create(transport.publish(errorMessage))
            .expectError(IllegalArgumentException::class.java)
            .verify()
    }

    @Test
    fun `should handle complex messages with metadata`() {
        val transport =
            PushpinTransport { message ->
                // Verify message structure in the transport
                assertEquals("complex-channel", message.channel)
                assertEquals("update", message.eventType)
                assertEquals(mapOf("key" to "value"), message.meta)
                Mono.just(true)
            }

        val message =
            Message.custom(
                channel = "complex-channel",
                data = mapOf("status" to "active"),
                eventType = "update",
                meta = mapOf("key" to "value"),
                transports = listOf(Transport.WebSocket, Transport.HttpStreamSSE),
            )

        StepVerifier
            .create(transport.publish(message))
            .expectNext(true)
            .verifyComplete()
    }
}
