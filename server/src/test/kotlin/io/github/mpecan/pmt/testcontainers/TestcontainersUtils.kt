package io.github.mpecan.pmt.testcontainers

import org.springframework.test.context.DynamicPropertyRegistry

/**
 * Utility class for integrating Pushpin testcontainers with Spring Boot tests.
 * * This utility provides convenient methods to:
 * - Create pre-configured Pushpin containers
 * - Configure Spring Boot properties to connect to Pushpin containers
 * - Support both single and multi-server test scenarios
 * * The utility automatically configures all necessary Spring properties including
 * server endpoints, ports, health checks, and test-specific settings.
 * * Example usage for single server:
 * ```kotlin
 * @Container
 * val pushpinContainer = TestcontainersUtils.createPushpinContainer(8080)
 * * @DynamicPropertySource
 * fun configureProperties(registry: DynamicPropertyRegistry) {
 *     TestcontainersUtils.configurePushpinProperties(registry, pushpinContainer)
 *     registry.add("server.port") { 8080 }
 * }
 * ```
 * * Example usage for multiple servers:
 * ```kotlin
 * @DynamicPropertySource
 * fun configureProperties(registry: DynamicPropertyRegistry) {
 *     TestcontainersUtils.configureMultiplePushpinProperties(
 *         registry, *         listOf(container1, container2),
 *         zmqEnabled = true
 *     )
 * }
 * ```
 */
object TestcontainersUtils {

    /**
     * Creates a new Pushpin container configured for the given host application port.
     * * This is a convenience method that creates a container with:
     * - The specified host application port exposed
     * - A simple catch-all route ("*") to the host application
     * - Default configuration suitable for testing
     * * @param hostPort The port your Spring Boot application runs on
     * @return A configured PushpinContainer ready to start
     */
    fun createPushpinContainer(hostPort: Int): PushpinContainer {
        return PushpinContainerBuilder()
            .withHostApplicationPort(hostPort)
            .withSimpleHostRoute()
            .build()
    }

    /**
     * Configures Spring Boot application properties for a single Pushpin container.
     * * This method sets up all necessary Spring properties to connect your application
     * to the Pushpin container, including:
     * - Server connection details (host, ports)
     * - Health check configuration
     * - Test mode settings
     * - Default timeouts
     * * The configured server will have ID "pushpin-test" at index 0.
     * * @param registry The Spring dynamic property registry
     * @param pushpinContainer The Pushpin container to configure properties for
     */
    fun configurePushpinProperties(registry: DynamicPropertyRegistry, pushpinContainer: PushpinContainer) {
        configurePushpinServer(registry, pushpinContainer, 0)
        configureCommonProperties(registry)
    }

    /**
     * Configures Spring Boot application properties for multiple Pushpin containers.
     * * This method sets up a multi-server Pushpin configuration, useful for testing:
     * - Load balancing scenarios
     * - Failover behavior
     * - Multi-region setups
     * - ZMQ vs HTTP transport comparison
     * * Each container is assigned:
     * - A unique server ID (pushpin-test-0, pushpin-test-1, etc.)
     * - Its own index in the servers array
     * - All necessary connection properties
     * * @param registry The Spring dynamic property registry
     * @param pushpinContainers List of Pushpin containers to configure
     * @param zmqEnabled Whether to enable ZMQ transport (default: true). *                   Set to false to use HTTP transport only.
     */
    fun configureMultiplePushpinProperties(
        registry: DynamicPropertyRegistry,
        pushpinContainers: List<PushpinContainer>,
        zmqEnabled: Boolean = true,
    ) {
        pushpinContainers.forEachIndexed { index, container ->
            configurePushpinServer(registry, container, index)
        }

        configureCommonProperties(registry)
        registry.add("pushpin.zmq-enabled") { zmqEnabled }
    }

    /**
     * Configures a single Pushpin server at the specified index.
     * This method encapsulates the common pattern of configuring server properties.
     */
    private fun configurePushpinServer(registry: DynamicPropertyRegistry, container: PushpinContainer, index: Int) {
        val serverId = if (index == 0) "pushpin-test" else "pushpin-test-$index"

        registry.add("pushpin.servers[$index].id") { serverId }
        registry.add("pushpin.servers[$index].host") { "localhost" }
        registry.add("pushpin.servers[$index].port") { container.getHttpPort() }
        registry.add("pushpin.servers[$index].publishPort") { container.getPublishPort() }
        registry.add("pushpin.servers[$index].controlPort") { container.getControlPort() }
        registry.add("pushpin.servers[$index].httpPort") { container.getHttpPublishPort() }
        registry.add("pushpin.servers[$index].active") { true }

        println(
            "Configured Pushpin server: $serverId with ports: HTTP=${container.getHttpPort()}, " +
                "Publish=${container.getPublishPort()}, Control=${container.getControlPort()}, " +
                "HttpPublish=${container.getHttpPublishPort()}",
        )
    }

    /**
     * Configures common properties for all test scenarios.
     */
    private fun configureCommonProperties(registry: DynamicPropertyRegistry) {
        // Enable health checks
        registry.add("pushpin.health-check-enabled") { true }
        registry.add("pushpin.health-check-interval") { 5000 }

        // Set timeouts for tests
        registry.add("pushpin.default-timeout") { 10000 }

        // Enable test mode
        registry.add("pushpin.test-mode") { true }
    }
}
