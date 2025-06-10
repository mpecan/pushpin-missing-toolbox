package io.github.mpecan.pmt.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import reactor.test.StepVerifier

class LongPollingControllerTest {
    private val controller = LongPollingController()

    @Test
    fun `should return long polling response for channel subscription`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should include GRIP headers for long polling`() {
        val channel = "test-channel"

        val response = controller.subscribe(channel)

        val headers = response.headers
        assertTrue(headers.containsKey("Grip-Hold"))
        assertTrue(headers.containsKey("Grip-Channel"))
        assertTrue(headers.containsKey("Grip-Timeout"))
        assertEquals("response", headers.getFirst("Grip-Hold"))
        assertEquals(channel, headers.getFirst("Grip-Channel"))
        assertEquals("20", headers.getFirst("Grip-Timeout"))
    }

    @Test
    fun `should return mono with timeout message`() {
        val channel = "polling-channel"

        val response = controller.subscribe(channel)
        val mono = response.body!!

        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["success"] == true &&
                    result["message"] == "No messages received within timeout period" &&
                    result["channel"] == channel
            }.verifyComplete()
    }

    @Test
    fun `should handle special characters in channel name`() {
        val channel = "polling-channel_123.events"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val mono = response.body!!
        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["channel"] == channel &&
                    result["success"] == true
            }.verifyComplete()
    }

    @Test
    fun `should handle empty channel name`() {
        val channel = ""

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("", response.headers.getFirst("Grip-Channel"))

        val mono = response.body!!
        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["channel"] == "" &&
                    result["success"] == true
            }.verifyComplete()
    }

    @Test
    fun `should have consistent response structure`() {
        val channel = "structure-test"

        val response = controller.subscribe(channel)
        val mono = response.body!!

        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result.containsKey("success") &&
                    result.containsKey("message") &&
                    result.containsKey("channel") &&
                    result["success"] is Boolean &&
                    result["message"] is String &&
                    result["channel"] is String &&
                    result.size == 3
            }.verifyComplete()
    }

    @Test
    fun `should handle channel names with spaces`() {
        val channel = "channel with spaces"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val mono = response.body!!
        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["channel"] == channel
            }.verifyComplete()
    }

    @Test
    fun `should use default timeout of 20 seconds`() {
        val channel = "timeout-test"

        val response = controller.subscribe(channel)

        assertEquals("20", response.headers.getFirst("Grip-Timeout"))
    }

    @Test
    fun `should handle unicode characters in channel name`() {
        val channel = "长轮询频道-⏰"

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val mono = response.body!!
        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["channel"] == channel &&
                    result["success"] == true
            }.verifyComplete()
    }

    @Test
    fun `should return same message for all channels`() {
        val channels = listOf("channel1", "channel2", "channel3")
        val expectedMessage = "No messages received within timeout period"

        channels.forEach { channel ->
            val response = controller.subscribe(channel)
            val mono = response.body!!

            StepVerifier
                .create(mono)
                .expectNextMatches { result ->
                    result["message"] == expectedMessage &&
                        result["success"] == true &&
                        result["channel"] == channel
                }.verifyComplete()
        }
    }

    @Test
    fun `should return OK status for all valid channel subscriptions`() {
        val testChannels =
            listOf(
                "simple-polling",
                "polling123",
                "UPPERCASE_POLLING",
                "mixed.Case-Polling_123",
            )

        testChannels.forEach { channel ->
            val response = controller.subscribe(channel)
            assertEquals(HttpStatus.OK, response.statusCode, "Failed for channel: $channel")
            assertNotNull(response.body, "Body should not be null for channel: $channel")
        }
    }

    @Test
    fun `should handle very long channel names`() {
        val channel = "long-polling-" + "a".repeat(500)

        val response = controller.subscribe(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val mono = response.body!!
        StepVerifier
            .create(mono)
            .expectNextMatches { result ->
                result["channel"] == channel
            }.verifyComplete()
    }

    @Test
    fun `should always return success true`() {
        val testChannels = listOf("test1", "test2", "test3")

        testChannels.forEach { channel ->
            val response = controller.subscribe(channel)
            val mono = response.body!!

            StepVerifier
                .create(mono)
                .expectNextMatches { result ->
                    result["success"] == true
                }.verifyComplete()
        }
    }
}
