package io.github.mpecan.pmt.transport.health

import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class TransportHealthCheckerTest {
    @Test
    fun `should check health of single server`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = true)
        val server = PushpinServer("test-server", "http://localhost:7999", 5561)

        val result = healthChecker.checkHealth(server)

        StepVerifier
            .create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should handle unhealthy server`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = false)
        val server = PushpinServer("test-server", "http://localhost:7999", 5561)

        val result = healthChecker.checkHealth(server)

        StepVerifier
            .create(result)
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `should check all servers when list is empty`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = true)

        val result = healthChecker.checkAllServers(emptyList())

        StepVerifier
            .create(result)
            .expectNext(emptyMap())
            .verifyComplete()
    }

    @Test
    fun `should check all servers when list has one server`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = true)
        val server = PushpinServer("server1", "http://localhost:7999", 5561)

        val result = healthChecker.checkAllServers(listOf(server))

        StepVerifier
            .create(result)
            .assertNext { healthMap ->
                assertEquals(1, healthMap.size)
                assertEquals(true, healthMap["server1"])
            }.verifyComplete()
    }

    @Test
    fun `should check all servers in parallel`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = true)
        val servers =
            listOf(
                PushpinServer("server1", "http://localhost:7999", 5561),
                PushpinServer("server2", "http://localhost:8000", 5562),
                PushpinServer("server3", "http://localhost:8001", 5563),
            )

        val result = healthChecker.checkAllServers(servers)

        StepVerifier
            .create(result)
            .assertNext { healthMap ->
                assertEquals(3, healthMap.size)
                assertEquals(true, healthMap["server1"])
                assertEquals(true, healthMap["server2"])
                assertEquals(true, healthMap["server3"])
            }.verifyComplete()
    }

    @Test
    fun `should check mixed healthy and unhealthy servers`() {
        val healthChecker =
            object : TransportHealthChecker {
                override fun checkHealth(server: PushpinServer): Mono<Boolean> =
                    when (server.id) {
                        "healthy-server" -> Mono.just(true)
                        "unhealthy-server" -> Mono.just(false)
                        else -> Mono.error(RuntimeException("Server error"))
                    }

                override fun getTransportType(): String = "test"
            }

        val servers =
            listOf(
                PushpinServer("healthy-server", "http://localhost:7999", 5561),
                PushpinServer("unhealthy-server", "http://localhost:8000", 5562),
                PushpinServer("error-server", "http://localhost:8001", 5563),
            )

        val result = healthChecker.checkAllServers(servers)

        StepVerifier
            .create(result)
            .assertNext { healthMap ->
                assertEquals(3, healthMap.size)
                assertEquals(true, healthMap["healthy-server"])
                assertEquals(false, healthMap["unhealthy-server"])
                assertEquals(false, healthMap["error-server"]) // Error returns false
            }.verifyComplete()
    }

    @Test
    fun `should handle individual server check errors in batch`() {
        val healthChecker =
            object : TransportHealthChecker {
                override fun checkHealth(server: PushpinServer): Mono<Boolean> =
                    when (server.id) {
                        "working-server" -> Mono.just(true)
                        else -> Mono.error(RuntimeException("Connection timeout"))
                    }

                override fun getTransportType(): String = "test"
            }

        val servers =
            listOf(
                PushpinServer("working-server", "http://localhost:7999", 5561),
                PushpinServer("broken-server", "http://localhost:8000", 5562),
            )

        val result = healthChecker.checkAllServers(servers)

        StepVerifier
            .create(result)
            .assertNext { healthMap ->
                assertEquals(2, healthMap.size)
                assertEquals(true, healthMap["working-server"])
                assertEquals(false, healthMap["broken-server"]) // Error mapped to false
            }.verifyComplete()
    }

    @Test
    fun `should return transport type`() {
        val healthChecker = TestTransportHealthChecker(alwaysHealthy = true)

        val transportType = healthChecker.getTransportType()

        assertEquals("test", transportType)
    }

    private class TestTransportHealthChecker(
        private val alwaysHealthy: Boolean,
    ) : TransportHealthChecker {
        override fun checkHealth(server: PushpinServer): Mono<Boolean> = Mono.just(alwaysHealthy)

        override fun getTransportType(): String = "test"
    }
}
