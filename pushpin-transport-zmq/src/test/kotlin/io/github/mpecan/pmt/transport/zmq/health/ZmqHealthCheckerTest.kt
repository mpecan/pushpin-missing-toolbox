package io.github.mpecan.pmt.transport.zmq.health

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import reactor.test.StepVerifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ZmqHealthCheckerTest {
    private lateinit var healthChecker: ZmqHealthChecker
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mockServerContext: ZContext
    private var mockServerThread: Thread? = null
    private var mockServerRunning = false

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        healthChecker = ZmqHealthChecker(objectMapper, 2000L)
        mockServerContext = ZContext()
    }

    @AfterEach
    fun tearDown() {
        // Stop mock server
        mockServerRunning = false
        mockServerThread?.interrupt()
        mockServerThread?.join(1000)

        // Clean up
        healthChecker.close()
        mockServerContext.close()
    }

    @Test
    fun `checkHealth should return true when server responds with valid stats`() {
        // Start a mock ZMQ REP server on control port
        val serverReady = CountDownLatch(1)
        val controlPort = 5564
        startMockStatsServer(controlPort) { request ->
            if (request == """{"method":"get-stats"}""") {
                """{"connections": 10, "messages": 100}"""
            } else {
                """{"error": "unknown method"}"""
            }
        }

        // Wait for server to start
        serverReady.await(2, TimeUnit.SECONDS)
        Thread.sleep(100) // Give it a moment to fully initialize

        // Given
        val server =
            PushpinServer(
                id = "server1",
                host = "localhost",
                port = 5561,
                controlPort = controlPort,
            )

        // When
        val result = healthChecker.checkHealth(server)

        // Then
        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `checkConnectivity should return true when socket can connect`() {
        // Start a mock ZMQ PULL server (simulating Pushpin's publish socket)
        val serverReady = CountDownLatch(1)
        startMockPullServer(5560)

        // Wait for server to start
        serverReady.await(2, TimeUnit.SECONDS)
        Thread.sleep(100)

        // Given
        val server =
            PushpinServer(
                id = "server1",
                host = "localhost",
                port = 5561,
                publishPort = 5560,
            )

        // When
        val result = healthChecker.checkConnectivity(server)

        // Then
        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `checkConnectivity should return true even when no server is listening`() {
        // Given - no server running
        // Note: ZMQ PUSH sockets will connect and send even if no PULL socket is listening
        // This is by design - ZMQ queues messages internally
        val server =
            PushpinServer(
                id = "server1",
                host = "localhost",
                port = 5561,
                publishPort = 5560,
            )

        // When
        val result = healthChecker.checkConnectivity(server)

        // Then
        StepVerifier
            .create(result)
            .expectNext(true) // ZMQ will return true even with no server
            .verifyComplete()
    }

    @Test
    fun `getTransportType should return zmq`() {
        // When & Then
        assert(healthChecker.getTransportType() == "zmq")
    }

    private fun startMockStatsServer(
        port: Int,
        responseHandler: (String) -> String,
    ) {
        mockServerRunning = true
        mockServerThread =
            Thread {
                val socket = mockServerContext.createSocket(SocketType.REP)
                try {
                    socket.bind("tcp://localhost:$port")

                    while (mockServerRunning && !Thread.currentThread().isInterrupted) {
                        val request = socket.recv(ZMQ.DONTWAIT)
                        if (request != null) {
                            val requestStr = String(request)
                            val response = responseHandler(requestStr)
                            socket.send(response.toByteArray(), 0)
                        }
                        Thread.sleep(10)
                    }
                } catch (e: Exception) {
                    if (mockServerRunning) {
                        e.printStackTrace()
                    }
                } finally {
                    socket.close()
                }
            }
        mockServerThread?.start()
    }

    private fun startMockPullServer(port: Int) {
        mockServerRunning = true
        mockServerThread =
            Thread {
                val socket = mockServerContext.createSocket(SocketType.PULL)
                try {
                    socket.bind("tcp://localhost:$port")

                    while (mockServerRunning && !Thread.currentThread().isInterrupted) {
                        // Just receive and discard messages
                        socket.recv(ZMQ.DONTWAIT)
                        Thread.sleep(10)
                    }
                } catch (e: Exception) {
                    if (mockServerRunning) {
                        e.printStackTrace()
                    }
                } finally {
                    socket.close()
                }
            }
        mockServerThread?.start()
    }
}
