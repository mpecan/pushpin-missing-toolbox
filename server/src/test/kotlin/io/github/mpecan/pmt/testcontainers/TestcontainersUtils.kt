package io.github.mpecan.pmt.testcontainers

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.Network

/**
 * Utility class for setting up testcontainers.
 */
object TestcontainersUtils {
    /**
     * Creates a new Pushpin container.
     */
    fun createPushpinContainer(hostPort: Int): PushpinContainer {
        return PushpinContainer().withHostPort(hostPort)
    }

    /**
     * Configures Spring Boot application properties to use the Pushpin container.
     */
    fun configurePushpinProperties(registry: DynamicPropertyRegistry, pushpinContainer: PushpinContainer) {
        // Configure a single Pushpin server
        registry.add("pushpin.servers[0].id") { "pushpin-test" }
        registry.add("pushpin.servers[0].host") { pushpinContainer.host }
        registry.add("pushpin.servers[0].port") { pushpinContainer.getHttpPort() }
        registry.add("pushpin.servers[0].publishPort") { pushpinContainer.getPublishPort() }
        registry.add("pushpin.servers[0].controlPort") { pushpinContainer.getControlPort() }
        registry.add("pushpin.servers[0].active") { true }

        // Enable health checks
        registry.add("pushpin.health-check-enabled") { true }
        registry.add("pushpin.health-check-interval") { 5000 }

        // Set a shorter timeout for tests
        registry.add("pushpin.default-timeout") { 2000 }

        // Enable test mode
        registry.add("pushpin.test-mode") { true }
    }
}
