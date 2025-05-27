package io.github.mpecan.pmt.client.serialization

import io.github.mpecan.pmt.client.formatter.HttpResponseMessageFormatter
import io.github.mpecan.pmt.client.formatter.HttpStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.LongPollingMessageFormatter
import io.github.mpecan.pmt.client.formatter.SSEStreamMessageFormatter
import io.github.mpecan.pmt.client.formatter.WebSocketMessageFormatter
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.model.Transport
import io.github.mpecan.pmt.model.HttpResponseFormat
import io.github.mpecan.pmt.model.HttpStreamFormat
import io.github.mpecan.pmt.model.PushpinMessage
import io.github.mpecan.pmt.model.WebSocketFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class DefaultMessageSerializerTest {

    private val webSocketFormatter: WebSocketMessageFormatter = mock()
    private val httpSseStreamFormatter: SSEStreamMessageFormatter = mock()
    private val httpStreamMessageFormatter: HttpStreamMessageFormatter = mock()
    private val httpResponseFormatter: HttpResponseMessageFormatter = mock()
    private val longPollingFormatter: LongPollingMessageFormatter = mock()

    private lateinit var messageSerializer: DefaultMessageSerializer

    @BeforeEach
    fun setUp() {
        messageSerializer = DefaultMessageSerializer(
            webSocketFormatter,
            httpSseStreamFormatter,
            httpStreamMessageFormatter,
            httpResponseFormatter,
            longPollingFormatter,
        )
    }

    @Test
    fun `serialize should use appropriate formatters based on message transports`() {
        // Given
        val message = Message(
            channel = "test-channel",
            data = "Hello, World!",
            id = "test-id",
            prevId = "test-prev-id",
            transports = listOf(Transport.WebSocket, Transport.HttpStreamSSE, Transport.HttpResponseSSE),
        )
        val webSocketFormat = WebSocketFormat(content = "ws-content", action = "send", type = "text")
        val httpStreamFormat = HttpStreamFormat(content = "http-stream-content", action = "send")
        val httpResponseFormat = HttpResponseFormat(body = "http-response-body")

        // Use doReturn().when() syntax for more reliable mocking
        org.mockito.kotlin.doReturn(webSocketFormat).`when`(webSocketFormatter).format(message)
        org.mockito.kotlin.doReturn(httpStreamFormat).`when`(httpSseStreamFormatter).format(message)
        org.mockito.kotlin.doReturn(httpResponseFormat).`when`(httpResponseFormatter).format(message)

        // When
        val result = messageSerializer.serialize(message)

        // Then
        // Create the expected result
        val expected = PushpinMessage(
            channel = "test-channel",
            id = "test-id",
            prevId = "test-prev-id",
            formats = mapOf(
                "ws-message" to webSocketFormat,
                "http-stream" to httpStreamFormat,
                "http-response" to httpResponseFormat,
            ),
        )

        // Compare the individual components
        assertEquals(expected.channel, result.channel)
        assertEquals(expected.id, result.id)
        assertEquals(expected.prevId, result.prevId)
        assertEquals(expected.formats["ws-message"], result.formats["ws-message"])
        assertEquals(expected.formats["http-stream"], result.formats["http-stream"])
        assertEquals(expected.formats["http-response"], result.formats["http-response"])
    }

    @Test
    fun `serialize should use HttpStream formatter when HttpStream transport is specified`() {
        // Given
        val message = Message(
            channel = "test-channel",
            data = "Hello, World!",
            transports = listOf(Transport.HttpStream),
        )
        val webSocketFormat = WebSocketFormat(content = "ws-content", action = "send", type = "text")
        val httpStreamFormat = HttpStreamFormat(content = "http-stream-content", action = "send")

        whenever(webSocketFormatter.format(message)).thenReturn(webSocketFormat)
        whenever(httpStreamMessageFormatter.format(message)).thenReturn(httpStreamFormat)

        // When
        val result = messageSerializer.serialize(message)

        // Then
        val expected = PushpinMessage(
            channel = "test-channel",
            formats = mapOf(
                "ws-message" to webSocketFormat,
                "http-stream" to httpStreamFormat,
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `serialize should use LongPolling formatter when LongPolling transport is specified`() {
        // Given
        val message = Message(
            channel = "test-channel",
            data = "Hello, World!",
            transports = listOf(Transport.LongPolling),
        )
        val webSocketFormat = WebSocketFormat(content = "ws-content", action = "send", type = "text")
        val longPollingFormat = HttpResponseFormat(body = "long-polling-body")

        whenever(webSocketFormatter.format(message)).thenReturn(webSocketFormat)
        whenever(longPollingFormatter.format(message)).thenReturn(longPollingFormat)

        // When
        val result = messageSerializer.serialize(message)

        // Then
        val expected = PushpinMessage(
            channel = "test-channel",
            formats = mapOf(
                "ws-message" to webSocketFormat,
                "http-response" to longPollingFormat,
            ),
        )
        assertEquals(expected, result)
    }
}
