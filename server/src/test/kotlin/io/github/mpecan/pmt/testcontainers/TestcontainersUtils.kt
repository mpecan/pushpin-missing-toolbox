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
     * Creates multiple Pushpin containers with a shared network.
     */
    fun createMultiplePushpinContainers(hostPorts: List<Int>, network: Network? = null): List<PushpinContainer> {
        return hostPorts.mapIndexed { index, hostPort ->
            val container = PushpinContainer()
                .withHostPort(hostPort)

            // If network is provided, add the container to the network
            if (network != null) {
                container.withNetwork(network)
                    .withNetworkAliases("pushpin-$index")
            }

            container
        }
    }

    /**
     * Configures Spring Boot application properties to use the Pushpin container.
     */
    fun configurePushpinProperties(registry: DynamicPropertyRegistry, pushpinContainer: PushpinContainer) {
        // Configure a single Pushpin server - using localhost for consistent connectivity
        registry.add("pushpin.servers[0].id") { "pushpin-test" }
        registry.add("pushpin.servers[0].host") { "localhost" } // Always use localhost for tests
        registry.add("pushpin.servers[0].port") { pushpinContainer.getMappedPort(PushpinContainer.HTTP_PORT) }
        registry.add("pushpin.servers[0].publishPort") { pushpinContainer.getMappedPort(PushpinContainer.PUBLISH_PORT) }
        registry.add("pushpin.servers[0].controlPort") { pushpinContainer.getMappedPort(PushpinContainer.CONTROL_PORT) }
        registry.add("pushpin.servers[0].httpPort") { pushpinContainer.getMappedPort(PushpinContainer.XPUB_PORT) }
        registry.add("pushpin.servers[0].active") { true }

        // Enable health checks
        registry.add("pushpin.health-check-enabled") { true }
        registry.add("pushpin.health-check-interval") { 5000 }

        // Set a shorter timeout for tests
        registry.add("pushpin.default-timeout") { 10000 } // Increased timeout for reliability

        // Enable test mode
        registry.add("pushpin.test-mode") { true }

        // Log the configuration
        println("Configured Pushpin test server:")
        println("  Host: localhost")
        println("  HTTP Port: ${pushpinContainer.getMappedPort(PushpinContainer.HTTP_PORT)}")
        println("  Publish Port: ${pushpinContainer.getMappedPort(PushpinContainer.PUBLISH_PORT)}")
        println("  Control Port: ${pushpinContainer.getMappedPort(PushpinContainer.XPUB_PORT)}")
    }

    /**
     * Configures Spring Boot application properties to use multiple Pushpin containers.
     */
    fun configureMultiplePushpinProperties(
        registry: DynamicPropertyRegistry,
        pushpinContainers: List<PushpinContainer>,
        zmqEnabled: Boolean = true
    ) {
        // Configure multiple Pushpin servers
        pushpinContainers.forEachIndexed { index, container ->
            // Use a unique ID based on the index
            val serverId = "pushpin-test-$index"
            registry.add("pushpin.servers[$index].id") { serverId }

            // Use localhost for connecting to the container
            registry.add("pushpin.servers[$index].host") { "localhost" }
            registry.add("pushpin.servers[$index].port") { container.getHttpPort() }
            registry.add("pushpin.servers[$index].publishPort") { container.getPublishPort() }
            registry.add("pushpin.servers[$index].controlPort") { container.getControlPort() }
            registry.add("pushpin.servers[$index].httpPort") { container.getHttpPublishPort() }
            registry.add("pushpin.servers[$index].active") { true }

            println("Configured Pushpin server: $serverId with ports: HTTP=${container.getHttpPort()}, " +
                    "Publish=${container.getPublishPort()}, Control=${container.getControlPort()}")
        }

        // Enable health checks
        registry.add("pushpin.health-check-enabled") { true }
        registry.add("pushpin.health-check-interval") { 5000 }

        // Set a shorter timeout for tests
        registry.add("pushpin.default-timeout") { 2000 }

        // Enable test mode
        registry.add("pushpin.test-mode") { true }

        // Enable ZMQ for multi-server setup
        registry.add("pushpin.zmq-enabled") { zmqEnabled }
    }
}
