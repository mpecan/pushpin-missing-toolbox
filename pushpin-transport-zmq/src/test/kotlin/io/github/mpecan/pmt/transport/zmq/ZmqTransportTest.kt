package io.github.mpecan.pmt.transport.zmq

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.model.PushpinMessage
import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier

class ZmqTransportTest {
    private lateinit var zmqTransportProperties: ZmqTransportProperties
    private lateinit var messageSerializer: MessageSerializer
    private lateinit var messageSerializationService: MessageSerializationService
    private lateinit var discoveryManager: PushpinDiscoveryManager
    private lateinit var zmqTransport: ZmqTransport

    @BeforeEach
    fun setup() {
        zmqTransportProperties =
            ZmqTransportProperties(
                connectionPoolEnabled = false, // Disable for easier testing
                hwm = 1000,
                linger = 1000,
                sendTimeout = 1000,
                reconnectIvl = 100,
                reconnectIvlMax = 0,
            )
        messageSerializer = mock()
        messageSerializationService = mock()
        discoveryManager = mock()

        zmqTransport =
            ZmqTransport(
                zmqTransportProperties,
                messageSerializer,
                messageSerializationService,
                discoveryManager,
            )
    }

    @Test
    fun `should handle empty server list gracefully`() {
        // Given
        val message = Message.simple("test-channel", "test-data")
        val pushpinMessage = PushpinMessage("test-channel", formats = emptyMap())

        whenever(messageSerializer.serialize(message)).thenReturn(pushpinMessage)
        whenever(discoveryManager.getAllServers()).thenReturn(emptyList())

        // When & Then
        StepVerifier
            .create(zmqTransport.publish(message))
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `should serialize message for ZMQ publishing`() {
        // Given
        val server = PushpinServer("test-server", "localhost", 7999, 5564, 8080, 5560, true)
        val message = Message.simple("test-channel", "test-data")
        val pushpinMessage = PushpinMessage("test-channel", formats = emptyMap())

        whenever(messageSerializer.serialize(message)).thenReturn(pushpinMessage)
        whenever(messageSerializationService.serialize(pushpinMessage)).thenReturn(
            "{\"channel\":\"test-channel\",\"formats\":[]}",
        )
        whenever(discoveryManager.getAllServers()).thenReturn(listOf(server))

        // When & Then
        StepVerifier
            .create(zmqTransport.publish(message))
            .expectNext(true)
            .verifyComplete()
    }
}
