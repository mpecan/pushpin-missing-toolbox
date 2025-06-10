package io.github.mpecan.pmt.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class HttpResponseControllerTest {
    private val controller = HttpResponseController()

    @Test
    fun `should return successful JSON response`() {
        val channel = "test-channel"

        val response = controller.getResponse(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val body = response.body!!
        assertEquals(true, body["success"])
        assertEquals("This is a non-streaming HTTP response", body["message"])
        assertEquals(channel, body["channel"])
    }

    @Test
    fun `should include GRIP channel header`() {
        val channel = "test-channel"

        val response = controller.getResponse(channel)

        val headers = response.headers
        assertTrue(headers.containsKey("Grip-Channel"))
        assertEquals(channel, headers.getFirst("Grip-Channel"))
    }

    @Test
    fun `should set correct content type`() {
        val channel = "test-channel"

        val response = controller.getResponse(channel)

        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
    }

    @Test
    fun `should handle different channel names`() {
        val testChannels =
            listOf(
                "simple-channel",
                "channel-123",
                "UPPERCASE_CHANNEL",
                "mixed.Case-Channel_456",
            )

        testChannels.forEach { channel ->
            val response = controller.getResponse(channel)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(channel, response.headers.getFirst("Grip-Channel"))

            val body = response.body!!
            assertEquals(channel, body["channel"])
            assertEquals(true, body["success"])
        }
    }

    @Test
    fun `should handle empty channel name`() {
        val channel = ""

        val response = controller.getResponse(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("", response.headers.getFirst("Grip-Channel"))

        val body = response.body!!
        assertEquals("", body["channel"])
        assertEquals(true, body["success"])
    }

    @Test
    fun `should handle special characters in channel name`() {
        val channel = "special-chars_123.test@domain"

        val response = controller.getResponse(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val body = response.body!!
        assertEquals(channel, body["channel"])
    }

    @Test
    fun `should handle unicode characters in channel name`() {
        val channel = "测试频道-🚀"

        val response = controller.getResponse(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val body = response.body!!
        assertEquals(channel, body["channel"])
        assertEquals(true, body["success"])
    }

    @Test
    fun `should always return consistent response structure`() {
        val channel = "consistency-test"

        val response = controller.getResponse(channel)
        val body = response.body!!

        // Verify all expected keys are present
        assertTrue(body.containsKey("success"))
        assertTrue(body.containsKey("message"))
        assertTrue(body.containsKey("channel"))

        // Verify data types
        assertTrue(body["success"] is Boolean)
        assertTrue(body["message"] is String)
        assertTrue(body["channel"] is String)

        assertEquals(3, body.size) // Should have exactly 3 keys
    }

    @Test
    fun `should handle very long channel names`() {
        val channel = "long-channel-" + "a".repeat(500)

        val response = controller.getResponse(channel)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(channel, response.headers.getFirst("Grip-Channel"))

        val body = response.body!!
        assertEquals(channel, body["channel"])
    }

    @Test
    fun `should return same message for all channels`() {
        val channels = listOf("channel1", "channel2", "channel3")
        val expectedMessage = "This is a non-streaming HTTP response"

        channels.forEach { channel ->
            val response = controller.getResponse(channel)
            val body = response.body!!

            assertEquals(expectedMessage, body["message"])
            assertEquals(true, body["success"])
        }
    }
}
