package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.metrics.MetricsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import reactor.test.StepVerifier

class HttpStreamingControllerTest {
    private lateinit var metricsService: MetricsService
    private lateinit var controller: HttpStreamingController

    @BeforeEach
    fun setUp() {
        metricsService = mock()
        controller = HttpStreamingController(metricsService)
    }

    @Test
    fun `should return HTTP streaming response for channel subscription`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should include GRIP headers for streaming`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        val headers = response.headers
        assertTrue(headers.containsKey("Grip-Hold"))
        assertTrue(headers.containsKey("Grip-Channel"))
        assertEquals("stream", headers.getFirst("Grip-Hold"))
        assertEquals(channel, headers.getFirst("Grip-Channel"))
    }

    @Test
    fun `should record metrics for connection events`() {
        val channel = "test-channel"

        controller.subscribe(channel)

        verify(metricsService).recordConnectionEvent("http-stream", "opened")
        verify(metricsService).incrementActiveConnections("http-stream")
    }

    @Test
    fun `should return flux with subscription confirmation message`() {
        val channel = "stream-channel"

        val response = controller.subscribe(channel)
        val flux = response.body!!

        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: stream-channel\n")
            .verifyComplete()
    }

    @Test
    fun `should handle special characters in channel name`() {
        val channel = "stream-channel_123.events"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val flux = response.body!!
        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: stream-channel_123.events\n")
            .verifyComplete()
    }

    @Test
    fun `should handle empty channel name`() {
        val channel = ""

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("", response.headers.getFirst("Grip-Channel"))

        verify(metricsService).recordConnectionEvent("http-stream", "opened")
        verify(metricsService).incrementActiveConnections("http-stream")
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
            .expectNext("Successfully subscribed to channel: channel with spaces\n")
            .verifyComplete()
    }

    @Test
    fun `should include newline character in streaming response`() {
        val channel = "newline-test"

        val response = controller.subscribe(channel)
        val flux = response.body!!

        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: newline-test\n")
            .verifyComplete()
    }

    @Test
    fun `should handle unicode characters in channel name`() {
        val channel = "流媒体频道-🌊"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val flux = response.body!!
        StepVerifier
            .create(flux)
            .expectNext("Successfully subscribed to channel: 流媒体频道-🌊\n")
            .verifyComplete()
    }

    @Test
    fun `should record metrics for each subscription`() {
        val channels = listOf("channel1", "channel2", "channel3")

        channels.forEach { channel ->
            controller.subscribe(channel)
        }

        // Verify metrics were recorded for each subscription (3 times each)
        verify(metricsService, times(3)).recordConnectionEvent("http-stream", "opened")
        verify(metricsService, times(3)).incrementActiveConnections("http-stream")
    }

    @Test
    fun `should return OK status for all valid channel subscriptions`() {
        val testChannels =
            listOf(
                "simple-stream",
                "stream123",
                "UPPERCASE_STREAM",
                "mixed.Case-Stream_123",
            )

        testChannels.forEach { channel ->
            val response = controller.subscribe(channel)
            assertEquals(HttpStatus.OK, response.statusCode, "Failed for channel: $channel")
            assertNotNull(response.body, "Body should not be null for channel: $channel")
        }
    }

    @Test
    fun `should handle very long channel names`() {
        val channel = "long-stream-" + "a".repeat(500)

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        verify(metricsService).recordConnectionEvent("http-stream", "opened")
        verify(metricsService).incrementActiveConnections("http-stream")
    }

    @Test
    fun `should use correct transport type for metrics`() {
        val channel = "metrics-test"

        controller.subscribe(channel)

        // Verify the exact transport type used in metrics
        verify(metricsService).recordConnectionEvent("http-stream", "opened")
        verify(metricsService).incrementActiveConnections("http-stream")
    }
}
