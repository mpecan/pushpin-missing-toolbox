package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.metrics.MetricsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

class WebSocketControllerTest {
    private lateinit var metricsService: MetricsService
    private lateinit var controller: WebSocketController

    @BeforeEach
    fun setUp() {
        metricsService = mock()
        controller = WebSocketController(metricsService)
    }

    @Test
    fun `should handle WebSocket OPEN event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordConnectionEvent("websocket", "opened")
        verify(metricsService).incrementActiveConnections("websocket")
    }

    @Test
    fun `should handle WebSocket TEXT event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "TEXT 5\r\nHello\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordMessageReceived("pushpin", "websocket")
    }

    @Test
    fun `should handle WebSocket BINARY event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "BINARY 3\r\nABC\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordMessageReceived("pushpin", "websocket")
    }

    @Test
    fun `should handle WebSocket PING event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "PING\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // PING should not record message metrics, only connection management
        verifyNoInteractions(metricsService)
    }

    @Test
    fun `should handle WebSocket PONG event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "PONG\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // PONG should not record any metrics
        verifyNoInteractions(metricsService)
    }

    @Test
    fun `should handle WebSocket CLOSE event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "CLOSE 4\r\n1000\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordConnectionEvent("websocket", "closed")
        verify(metricsService).decrementActiveConnections("websocket")
    }

    @Test
    fun `should handle WebSocket DISCONNECT event`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "DISCONNECT\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordConnectionEvent("websocket", "disconnected")
        verify(metricsService).decrementActiveConnections("websocket")
    }

    @Test
    fun `should handle multiple events in single request`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "TEXT 5\r\nHello\r\nTEXT 5\r\nWorld\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        // Should record metrics for each text message (2 TEXT events)
        verify(metricsService, times(2)).recordMessageReceived("pushpin", "websocket")
    }

    @Test
    fun `should handle empty channel name`() {
        val channel = ""
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordConnectionEvent("websocket", "opened")
        verify(metricsService).incrementActiveConnections("websocket")
    }

    @Test
    fun `should handle special characters in channel name`() {
        val channel = "websocket-channel_123.events"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        verify(metricsService).recordConnectionEvent("websocket", "opened")
        verify(metricsService).incrementActiveConnections("websocket")
    }

    @Test
    fun `should use default content type when not specified`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events", // This is the default
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun `should handle unicode characters in channel name`() {
        val channel = "WebSocket频道-🔌"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        verify(metricsService).recordConnectionEvent("websocket", "opened")
        verify(metricsService).incrementActiveConnections("websocket")
    }

    @Test
    fun `should include WebSocket response headers`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val responseHeaders = response.headers
        // WebSocket responses should include appropriate headers
        assertEquals("application/websocket-events", responseHeaders.getFirst("Content-Type"))
    }

    @Test
    fun `should handle connection with meta headers`() {
        val channel = "test-channel"
        val connectionId = "conn-123"
        val secWebSocketKey = "test-key"
        val headers = HttpHeaders()
        headers.add("Meta-Test-Header", "test-value")
        val body = "OPEN\r\n\r\n"

        val response =
            controller.handleWebSocketOverHttp(
                channel = channel,
                connectionId = connectionId,
                contentType = "application/websocket-events",
                secWebSocketAccept = secWebSocketKey,
                headers = headers,
                body = body,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        verify(metricsService).recordConnectionEvent("websocket", "opened")
        verify(metricsService).incrementActiveConnections("websocket")
    }
}
