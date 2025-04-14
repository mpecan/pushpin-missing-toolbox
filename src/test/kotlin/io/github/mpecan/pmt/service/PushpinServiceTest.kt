package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.formatter.*
import io.github.mpecan.pmt.model.Message
import io.github.mpecan.pmt.model.PushpinFormat
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.model.Transport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class PushpinServiceTest {

    @BeforeEach
    fun initMocks() {
        // Make all mocks lenient to avoid "unnecessary stubbings" errors
        Mockito.lenient().`when`(webClient.post()).thenReturn(requestBodyUriSpec)
        Mockito.lenient().`when`(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec)
        Mockito.lenient().`when`(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
        Mockito.lenient().`when`(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec)
        Mockito.lenient().`when`(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }


    @Mock
    private lateinit var webClient: WebClient

    @Mock
    private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

    @Mock
    private lateinit var requestBodySpec: WebClient.RequestBodySpec

    @Mock
    private lateinit var responseSpec: WebClient.ResponseSpec

    @Mock
    private lateinit var webSocketFormatter: WebSocketMessageFormatter

    @Mock
    private lateinit var httpSseStreamFormatter: SSEStreamMessageFormatter

    @Mock
    private lateinit var httpStreamFormatter: HttpStreamMessageFormatter

    @Mock
    private lateinit var httpResponseFormatter: HttpResponseMessageFormatter

    @Mock
    private lateinit var longPollingFormatter: LongPollingMessageFormatter

    @Mock
    private lateinit var discoveryManager: PushpinDiscoveryManager

    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var pushpinService: PushpinService

    private val server1 = PushpinServer(
        id = "test-server-1",
        host = "localhost",
        port = 7999,
        active = true
    )

    @BeforeEach
    fun setUp() {
        // Create test properties
        val serverProps1 = PushpinProperties.ServerProperties(
            id = "test-server-1",
            host = "localhost",
            port = 7999,
            active = true
        )

        val serverProps2 = PushpinProperties.ServerProperties(
            id = "test-server-2",
            host = "localhost",
            port = 7998,
            active = true
        )

        pushpinProperties = PushpinProperties(
            servers = listOf(serverProps1, serverProps2),
            healthCheckEnabled = false,
            defaultTimeout = 5000
        )

        // Create a test message for mocking
        val testMessage = Message.simple("test-channel", "test-data")

        // Set up formatters with concrete message instance - using lenient to avoid "unnecessary stubbings" errors
        Mockito.lenient().`when`(webSocketFormatter.format(testMessage))
            .thenReturn(PushpinFormat(content = "ws-content", action = "send", type = "text"))

        Mockito.lenient().`when`(httpSseStreamFormatter.format(testMessage))
            .thenReturn(PushpinFormat(content = "sse-content", action = "send"))

        Mockito.lenient().`when`(httpStreamFormatter.format(testMessage))
            .thenReturn(PushpinFormat(content = "http-stream-content", action = "send"))

        Mockito.lenient().`when`(httpResponseFormatter.format(testMessage))
            .thenReturn(PushpinFormat(body = "http-response-body"))

        Mockito.lenient().`when`(longPollingFormatter.format(testMessage))
            .thenReturn(PushpinFormat(body = "long-polling-body"))

        // Create messages with different transports for specific tests
        val httpStreamMessage = Message(
            channel = "test-channel",
            data = "test-data",
            transports = listOf(Transport.HttpStream)
        )

        val longPollingMessage = Message(
            channel = "test-channel",
            data = "test-data",
            transports = listOf(Transport.LongPolling)
        )

        // Set up formatters for specific transport messages - using lenient to avoid "unnecessary stubbings" errors
        Mockito.lenient().`when`(webSocketFormatter.format(httpStreamMessage))
            .thenReturn(PushpinFormat(content = "ws-content", action = "send", type = "text"))

        Mockito.lenient().`when`(httpStreamFormatter.format(httpStreamMessage))
            .thenReturn(PushpinFormat(content = "http-stream-content", action = "send"))

        Mockito.lenient().`when`(httpResponseFormatter.format(httpStreamMessage))
            .thenReturn(PushpinFormat(body = "http-response-body"))

        Mockito.lenient().`when`(webSocketFormatter.format(longPollingMessage))
            .thenReturn(PushpinFormat(content = "ws-content", action = "send", type = "text"))

        Mockito.lenient().`when`(httpSseStreamFormatter.format(longPollingMessage))
            .thenReturn(PushpinFormat(content = "sse-content", action = "send"))

        Mockito.lenient().`when`(longPollingFormatter.format(longPollingMessage))
            .thenReturn(PushpinFormat(body = "long-polling-body"))

        // Create service with mocked dependencies
        pushpinService = PushpinService(
            pushpinProperties,
            discoveryManager,
            webSocketFormatter,
            httpSseStreamFormatter,
            httpStreamFormatter,
            httpResponseFormatter,
            longPollingFormatter
        )

        // Use reflection to replace the webClient with our mock
        val webClientField = PushpinService::class.java.getDeclaredField("webClient")
        webClientField.isAccessible = true
        webClientField.set(pushpinService, webClient)
    }

    @Test
    fun `getAllServers should return all configured servers`() {
        `when`(discoveryManager.getAllServers()).thenReturn(
            pushpinProperties.servers.map {
                it.toPushpinServer()
            }
        )
        val servers = pushpinService.getAllServers()

        assertEquals(2, servers.size)
        assertTrue(servers.any { it.id == "test-server-1" })
        assertTrue(servers.any { it.id == "test-server-2" })
    }

    @Test
    fun `getServerById should return the correct server`() {
        `when`(discoveryManager.getServerById("test-server-1")).thenReturn(pushpinProperties.servers[0].toPushpinServer())
        val server = pushpinService.getServerById("test-server-1")

        assertTrue(server != null)
        assertEquals("test-server-1", server.id)
        assertEquals("localhost", server.host)
        assertEquals(7999, server.port)
    }

    @Test
    fun `getServerById should return null for non-existent server`() {
        val server = pushpinService.getServerById("non-existent")

        assertTrue(server == null)
    }

    @Test
    fun `publishMessage should return true on successful response`() {
        // Set up the discovery manager to return a server
        `when`(discoveryManager.getAllServers()).thenReturn(listOf(server1))

        // Set up the WebClient to return a successful response
        `when`(responseSpec.onStatus(any(), any())).thenReturn(responseSpec)

        // Use ParameterizedTypeReference for bodyToMono to match the actual method call
        `when`(responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>())).thenReturn(Mono.just("success"))

        // Create a test message
        val message = Message.simple("test-channel", "test-data")

        // Call the method
        val result = pushpinService.publishMessage(message).block()

        // Verify the result
        assertTrue(result!!)
    }

    @Test
    fun `publishMessage should return false on error response`() {
        // Set up the discovery manager to return a server
        `when`(discoveryManager.getAllServers()).thenReturn(listOf(server1))

        // Set up the WebClient to return an error response
        `when`(responseSpec.onStatus(any(), any())).thenReturn(responseSpec)

        // Use ParameterizedTypeReference for bodyToMono to match the actual method call
        `when`(responseSpec.bodyToMono(any<ParameterizedTypeReference<String>>())).thenReturn(Mono.error(RuntimeException("Test error")))

        // Create a test message
        val message = Message.simple("test-channel", "test-data")

        // Call the method
        val result = pushpinService.publishMessage(message).block()

        // Verify the result
        assertFalse(result!!)
    }

    @Test
    fun `publishMessage should return error when no servers available`() {
        // Set up the discovery manager to return no servers
        `when`(discoveryManager.getAllServers()).thenReturn(emptyList())

        // Create a test message
        val message = Message.simple("test-channel", "test-data")

        // Call the method and verify it returns an error
        val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            pushpinService.publishMessage(message).block()
        }

        // Verify the error message
        assertEquals("No Pushpin servers available", exception.message)
    }

    @Test
    fun `toPushPin should use HttpStream formatter when HttpStream transport is specified`() {
        // Create a message with HttpStream transport
        val message = Message(
            channel = "test-channel",
            data = "test-data",
            transports = listOf(Transport.HttpStream)
        )

        // Get the toPushPin method using reflection
        val toPushPinMethod = PushpinService::class.java.getDeclaredMethod("toPushPin", Message::class.java)
        toPushPinMethod.isAccessible = true

        // Call the method
        toPushPinMethod.invoke(pushpinService, message)

        // Verify the formatters were called correctly
        verify(webSocketFormatter).format(message)
        verify(httpStreamFormatter).format(message)
        verify(httpResponseFormatter).format(message)
    }

    @Test
    fun `toPushPin should use LongPolling formatter when LongPolling transport is specified`() {
        // Create a message with LongPolling transport
        val message = Message(
            channel = "test-channel",
            data = "test-data",
            transports = listOf(Transport.LongPolling)
        )

        // Get the toPushPin method using reflection
        val toPushPinMethod = PushpinService::class.java.getDeclaredMethod("toPushPin", Message::class.java)
        toPushPinMethod.isAccessible = true

        // Call the method
        toPushPinMethod.invoke(pushpinService, message)

        // Verify the formatters were called correctly
        verify(webSocketFormatter).format(message)
        verify(httpSseStreamFormatter).format(message)
        verify(longPollingFormatter).format(message)
    }

    @Test
    fun `toPushPin should use default formatters when no specific transports are specified`() {
        // Create a message with default transports - use the same message that we mocked in setUp
        val message = Message.simple("test-channel", "test-data")

        // Get the toPushPin method using reflection
        val toPushPinMethod = PushpinService::class.java.getDeclaredMethod("toPushPin", Message::class.java)
        toPushPinMethod.isAccessible = true

        // Call the method
        toPushPinMethod.invoke(pushpinService, message)

        // Verify the formatters were called correctly
        // Use Mockito.verify() to avoid any issues with the verify method
        verify(webSocketFormatter).format(message)
        verify(httpSseStreamFormatter).format(message)

        // The default transports include LongPolling, so longPollingFormatter should be used instead of httpResponseFormatter
        verify(longPollingFormatter).format(message)
    }
}
