package io.github.mpecan.pmt.testcontainers

import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for integration tests that require a Pushpin server.
 *
 * This class provides the base annotations for Spring Boot and Testcontainers.
 * Each subclass should define its own companion object with the container configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class PushpinIntegrationTest
