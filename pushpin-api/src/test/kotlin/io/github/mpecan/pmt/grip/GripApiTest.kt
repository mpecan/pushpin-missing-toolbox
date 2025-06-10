package io.github.mpecan.pmt.grip

import io.github.mpecan.pmt.grip.websocket.WebSocketEventType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GripApiTest {
    // A test key that has over 256 bits
    val testKey = Random.nextBytes(32).joinToString("") { "%02x".format(it) }

    @Test
    fun `headers() should return new GripHeaderBuilder instance`() {
        val builder1 = GripApi.headers()
        val builder2 = GripApi.headers()

        // Verify new instances are created each time
        assertTrue(builder1 !== builder2)
    }

    @Test
    fun `websocket() should return new WebSocketMessageBuilder instance`() {
        val builder1 = GripApi.websocket()
        val builder2 = GripApi.websocket()

        // Verify new instances are created each time
        assertTrue(builder1 !== builder2)
    }

    @Test
    fun `parseWebSocketEvents() should parse WebSocket events`() {
        // This test verifies the method delegates correctly
        // Since we can't mock the static method without additional libraries,
        // we'll test with actual parsing
        val body = "TEXT 5\r\nhello\r\n"

        val result = GripApi.parseWebSocketEvents(body)

        assertEquals(1, result.size)
        assertEquals(WebSocketEventType.TEXT, result[0].type)
        assertEquals("hello", result[0].content)
    }

    @Test
    fun `createSignature() should create GRIP signature`() {
        // This test verifies the method delegates correctly
        // We'll test that a signature is created (non-empty string)
        val result = GripApi.createSignature("test-issuer", testKey, 3600L)

        assertTrue(result.isNotEmpty())
        // JWT signatures typically have 3 parts separated by dots
        assertEquals(3, result.split(".").size)
    }

    @Test
    fun `createSignature() without expiry should create GRIP signature`() {
        val result = GripApi.createSignature("test-issuer", testKey)

        assertTrue(result.isNotEmpty())
        assertEquals(3, result.split(".").size)
    }

    @Test
    fun `validateSignature() should validate GRIP signature`() {
        // Create a signature and validate it
        val signature = GripApi.createSignature("test-issuer", testKey)
        val result = GripApi.validateSignature(signature, testKey)

        assertTrue(result)
    }

    @Test
    fun `longPollingResponse() should create correct ResponseEntity builder`() {
        val builder = GripApi.longPollingResponse<Any>("test-channel", 30)

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.APPLICATION_JSON)
            .hasHeader(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_RESPONSE)
            .hasHeader(GripConstants.HEADER_GRIP_CHANNEL, "test-channel")
            .hasHeader(GripConstants.HEADER_GRIP_TIMEOUT, "30")
    }

    @Test
    fun `longPollingResponse() with default timeout should use default value`() {
        val builder = GripApi.longPollingResponse<Any>("test-channel")

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.APPLICATION_JSON)
            .hasHeader(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_RESPONSE)
            .hasHeader(GripConstants.HEADER_GRIP_CHANNEL, "test-channel")
            .hasHeader(GripConstants.HEADER_GRIP_TIMEOUT, GripConstants.DEFAULT_TIMEOUT.toString())
    }

    @Test
    fun `streamingResponse() should create correct ResponseEntity builder`() {
        val builder = GripApi.streamingResponse("stream-channel")

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.TEXT_PLAIN)
            .hasHeader(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_STREAM)
            .hasHeader(GripConstants.HEADER_GRIP_CHANNEL, "stream-channel")
            .doesNotHaveHeader(GripConstants.HEADER_GRIP_TIMEOUT)
    }

    @Test
    fun `sseResponse() should create correct ResponseEntity builder`() {
        val builder = GripApi.sseResponse("sse-channel")

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.TEXT_EVENT_STREAM)
            .hasHeader(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_STREAM)
            .hasHeader(GripConstants.HEADER_GRIP_CHANNEL, "sse-channel")
            .doesNotHaveHeader(GripConstants.HEADER_GRIP_TIMEOUT)
    }

    @Test
    fun `websocketResponse() should create correct ResponseEntity builder`() {
        val builder = GripApi.websocketResponse("test-accept-key", 60)

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.parseMediaType(GripConstants.CONTENT_TYPE_WEBSOCKET_EVENTS))
            .hasHeader(GripConstants.HEADER_SEC_WEBSOCKET_EXTENSIONS, GripConstants.WEBSOCKET_EXTENSION_GRIP)
            .hasHeader(GripConstants.HEADER_SEC_WEBSOCKET_ACCEPT, "test-accept-key")
            .hasHeader(GripConstants.HEADER_KEEP_ALIVE_INTERVAL, "60")
    }

    @Test
    fun `websocketResponse() with default keep-alive should use default value`() {
        val builder = GripApi.websocketResponse("test-accept-key")

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.parseMediaType(GripConstants.CONTENT_TYPE_WEBSOCKET_EVENTS))
            .hasHeader(GripConstants.HEADER_SEC_WEBSOCKET_EXTENSIONS, GripConstants.WEBSOCKET_EXTENSION_GRIP)
            .hasHeader(GripConstants.HEADER_SEC_WEBSOCKET_ACCEPT, "test-accept-key")
            .hasHeader(GripConstants.HEADER_KEEP_ALIVE_INTERVAL, GripConstants.DEFAULT_KEEP_ALIVE_INTERVAL.toString())
    }

    @Test
    fun `extractMetaHeaders() should extract headers with Meta- prefix`() {
        val headers =
            HttpHeaders().apply {
                add("Meta-User-Id", "12345")
                add("Meta-Session-Id", "abc-def")
                add("Content-Type", "application/json")
                add("meta-lowercase", "should-work")
            }

        val metaHeaders = GripApi.extractMetaHeaders(headers)

        assertEquals(3, metaHeaders.size)
        assertEquals("12345", metaHeaders["User-Id"])
        assertEquals("abc-def", metaHeaders["Session-Id"])
        assertEquals("should-work", metaHeaders["lowercase"])
    }

    @Test
    fun `extractMetaHeaders() should handle empty headers`() {
        val headers = HttpHeaders()

        val metaHeaders = GripApi.extractMetaHeaders(headers)

        assertTrue(metaHeaders.isEmpty())
    }

    @Test
    fun `extractMetaHeaders() should handle headers without meta prefix`() {
        val headers =
            HttpHeaders().apply {
                add("Content-Type", "application/json")
                add("Authorization", "Bearer token")
            }

        val metaHeaders = GripApi.extractMetaHeaders(headers)

        assertTrue(metaHeaders.isEmpty())
    }

    @Test
    fun `extractMetaHeaders() should use first value for multi-value headers`() {
        val headers =
            HttpHeaders().apply {
                add("Meta-Test", "first")
                add("Meta-Test", "second")
            }

        val metaHeaders = GripApi.extractMetaHeaders(headers)

        assertEquals("first", metaHeaders["Test"])
    }

    @Test
    fun `applyMetaHeaders() should add Set-Meta- prefixed headers`() {
        val metaHeaders =
            mapOf(
                "User-Id" to "12345",
                "Session-Id" to "abc-def",
                "Custom-Value" to "test",
            )

        val builder = GripApi.longPollingResponse<Any>("channel")
        val result = GripApi.applyMetaHeaders(builder, metaHeaders)

        assertTrue(builder === result) // Should return same builder instance
        assertThat(result)
            .hasHeader("${GripConstants.HEADER_SET_META_PREFIX}User-Id", "12345")
            .hasHeader("${GripConstants.HEADER_SET_META_PREFIX}Session-Id", "abc-def")
            .hasHeader("${GripConstants.HEADER_SET_META_PREFIX}Custom-Value", "test")
    }

    @Test
    fun `applyMetaHeaders() should handle empty meta headers`() {
        val builder = GripApi.longPollingResponse<Any>("channel")
        val result = GripApi.applyMetaHeaders(builder, emptyMap())

        assertTrue(builder === result)
        assertThat(result)
            .withHeaders { headers ->
                assertTrue(headers.entries.none { it.key.startsWith(GripConstants.HEADER_SET_META_PREFIX) })
            }
    }

    @Test
    fun `integration test - complete long polling setup`() {
        val metaHeaders = mapOf("User-Id" to "user123")

        val builder = GripApi.longPollingResponse<Any>("notifications", 45)
        GripApi.applyMetaHeaders(builder, metaHeaders)

        assertThat(builder)
            .hasStatus(HttpStatus.OK)
            .hasContentType(MediaType.APPLICATION_JSON)
            .hasHeaders(
                GripConstants.HEADER_GRIP_HOLD to GripConstants.HOLD_MODE_RESPONSE,
                GripConstants.HEADER_GRIP_CHANNEL to "notifications",
                GripConstants.HEADER_GRIP_TIMEOUT to "45",
                "${GripConstants.HEADER_SET_META_PREFIX}User-Id" to "user123",
            )
    }
}
