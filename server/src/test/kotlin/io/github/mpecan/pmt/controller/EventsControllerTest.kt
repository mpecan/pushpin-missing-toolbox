package io.github.mpecan.pmt.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import reactor.test.StepVerifier

class EventsControllerTest {
    private val controller = EventsController()

    @Test
    fun `should return SSE response for channel subscription`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should include GRIP headers in response`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        val headers = response.headers
        assertTrue(headers.containsKey("Grip-Hold"))
        assertTrue(headers.containsKey("Grip-Channel"))
        assertEquals("stream", headers.getFirst("Grip-Hold"))
        assertEquals(channel, headers.getFirst("Grip-Channel"))
    }

    @Test
    fun `should set correct content type for SSE`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        val contentType = response.headers.contentType
        assertEquals(MediaType.TEXT_EVENT_STREAM, contentType)
    }

    @Test
    fun `should return flux with subscription confirmation message`() {
        val channel = "news-channel"

        val response = controller.subscribe(channel)
        val flux = response.body!!

        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: news-channel")
            .verifyComplete()
    }

    @Test
    fun `should handle special characters in channel name`() {
        val channel = "test-channel_123.events"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val flux = response.body!!
        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: test-channel_123.events")
            .verifyComplete()
    }

    @Test
    fun `should handle empty channel name`() {
        val channel = ""

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("", response.headers.getFirst("Grip-Channel"))
    }

    @Test
    fun `should handle channel names with spaces`() {
        val channel = "channel with spaces"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val flux = response.body!!
        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: channel with spaces")
            .verifyComplete()
    }

    @Test
    fun `should handle very long channel names`() {
        val channel = "a".repeat(1000)

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))
    }

    @Test
    fun `should handle unicode characters in channel name`() {
        val channel = "测试频道-🎉"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val flux = response.body!!
        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: 测试频道-🎉")
            .verifyComplete()
    }

    @Test
    fun `should return OK status for all valid channel subscriptions`() {
        val testChannels =
            listOf(
                "simple-channel",
                "channel123",
                "UPPERCASE_CHANNEL",
                "mixed.Case-Channel_123",
            )

        testChannels.forEach { channel ->
            val response = controller.subscribe(channel)
            assertEquals(HttpStatus.OK, response.statusCode, "Failed for channel: $channel")
            assertNotNull(response.body, "Body should not be null for channel: $channel")
        }
    }
}
