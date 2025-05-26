package io.github.mpecan.pmt.transport.http

import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

class HttpHealthCheckerTest {

    private lateinit var webClient: WebClient
    private lateinit var requestSpec: RequestHeadersUriSpec<*>
    private lateinit var responseSpec: ResponseSpec
    private lateinit var healthChecker: HttpHealthChecker

    @BeforeEach
    fun setUp() {
        webClient = mock()
        requestSpec = mock<RequestHeadersUriSpec<*>>()
        responseSpec = mock()
        healthChecker = HttpHealthChecker(webClient)
    }

    @Test
    fun `checkHealth should return true when server responds successfully`() {
        // Given
        val server = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 5561,
            healthCheckPath = "/api/health/check",
        )

        whenever(webClient.get()).thenReturn(requestSpec)
        whenever(requestSpec.uri("http://localhost:5561/api/health/check")).thenReturn(requestSpec)
        whenever(requestSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.bodyToMono(String::class.java)).thenReturn(Mono.just("OK"))

        // When
        val result = healthChecker.checkHealth(server)

        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `checkHealth should return false when server returns error`() {
        // Given
        val server = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 5561,
        )

        whenever(webClient.get()).thenReturn(requestSpec)
        whenever(requestSpec.uri(any<String>())).thenReturn(requestSpec)
        whenever(requestSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.bodyToMono(String::class.java)).thenReturn(
            Mono.error(RuntimeException("Connection refused")),
        )

        // When
        val result = healthChecker.checkHealth(server)

        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `checkHealth should timeout after specified duration`() {
        // Given
        val server = PushpinServer(
            id = "server1",
            host = "localhost",
            port = 5561,
        )
        val healthCheckerWithShortTimeout = HttpHealthChecker(webClient, 100L)

        whenever(webClient.get()).thenReturn(requestSpec)
        whenever(requestSpec.uri(any<String>())).thenReturn(requestSpec)
        whenever(requestSpec.retrieve()).thenReturn(responseSpec)
        whenever(responseSpec.bodyToMono(String::class.java)).thenReturn(
            Mono.just("OK").delayElement(Duration.ofMillis(200)),
        )

        // When
        val result = healthCheckerWithShortTimeout.checkHealth(server)

        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `getTransportType should return http`() {
        // When & Then
        assert(healthChecker.getTransportType() == "http")
    }

    @Test
    fun `checkAllServers should check multiple servers in parallel`() {
        // Given
        val server1 = PushpinServer("server1", "host1", 5561)
        val server2 = PushpinServer("server2", "host2", 5561)
        val servers = listOf(server1, server2)

        // Create separate mocks for each server
        val requestSpec1: RequestHeadersUriSpec<*> = mock()
        val requestSpec2: RequestHeadersUriSpec<*> = mock()
        val responseSpec1: ResponseSpec = mock()
        val responseSpec2: ResponseSpec = mock()

        whenever(webClient.get())
            .thenReturn(requestSpec1)
            .thenReturn(requestSpec2)

        // Server1 setup - healthy
        whenever(requestSpec1.uri("http://host1:5561/api/health/check")).thenReturn(requestSpec1)
        whenever(requestSpec1.retrieve()).thenReturn(responseSpec1)
        whenever(responseSpec1.bodyToMono(String::class.java)).thenReturn(Mono.just("OK"))

        // Server2 setup - unhealthy
        whenever(requestSpec2.uri("http://host2:5561/api/health/check")).thenReturn(requestSpec2)
        whenever(requestSpec2.retrieve()).thenReturn(responseSpec2)
        whenever(responseSpec2.bodyToMono(String::class.java)).thenReturn(Mono.error(RuntimeException("Error")))

        // When
        val result = healthChecker.checkAllServers(servers)

        // Then
        StepVerifier.create(result)
            .expectNextMatches { map ->
                map.size == 2 && map["server1"] == true && map["server2"] == false
            }
            .verifyComplete()
    }

    @Test
    fun `checkAllServers should return empty map for empty server list`() {
        // When
        val result = healthChecker.checkAllServers(emptyList())

        // Then
        StepVerifier.create(result)
            .expectNext(emptyMap())
            .verifyComplete()
    }
}
