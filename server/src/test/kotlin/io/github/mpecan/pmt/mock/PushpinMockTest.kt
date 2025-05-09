package io.github.mpecan.pmt.mock

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

/**
 * Base class for integration tests that use MockMvc and MockRestServiceServer to simulate a Pushpin server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PushpinMockTest.TestConfig::class)
abstract class PushpinMockTest {

    /**
     * Test configuration that provides a RestTemplate bean.
     */
    @TestConfiguration
    class TestConfig {
        @Bean
        fun restTemplate(): RestTemplate {
            return RestTemplate()
        }
    }

    companion object {
        private const val PUSHPIN_PORT = 7999
        private const val CONTROL_PORT = 5564

        /**
         * Configure Spring Boot application properties to use the mock Pushpin server.
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Configure a single Pushpin server
            registry.add("pushpin.servers[0].id") { "pushpin-mock" }
            registry.add("pushpin.servers[0].host") { "localhost" }
            registry.add("pushpin.servers[0].port") { PUSHPIN_PORT }
            registry.add("pushpin.servers[0].controlPort") { CONTROL_PORT }
            registry.add("pushpin.servers[0].publishPort") { CONTROL_PORT }
            registry.add("pushpin.servers[0].active") { true }

            // Enable health checks
            registry.add("pushpin.health-check-enabled") { true }
            registry.add("pushpin.health-check-interval") { 1000 }

            // Set a shorter timeout for tests
            registry.add("pushpin.default-timeout") { 1000 }
        }
    }

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var restTemplate: RestTemplate

    protected lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate)

        // Set up mock for health check
        mockServer.expect(requestTo("http://localhost:$PUSHPIN_PORT/status"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body("OK"))

        // Set up mock for publish endpoint
        mockServer.expect(requestTo("http://localhost:$CONTROL_PORT/publish"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}"))
    }

    /**
     * Sets up a mock response for the health check endpoint.
     */
    protected fun mockHealthCheck(status: HttpStatus = HttpStatus.OK) {
        mockServer.expect(requestTo("http://localhost:$PUSHPIN_PORT/status"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(if (status == HttpStatus.OK) "OK" else "Error"))
    }

    /**
     * Sets up a mock response for the publish endpoint.
     */
    protected fun mockPublish(status: HttpStatus = HttpStatus.OK) {
        mockServer.expect(requestTo("http://localhost:$CONTROL_PORT/publish"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}"))
    }

    /**
     * Verifies that all expected requests were made.
     */
    protected fun verifyAllExpectations() {
        mockServer.verify()
    }
}
