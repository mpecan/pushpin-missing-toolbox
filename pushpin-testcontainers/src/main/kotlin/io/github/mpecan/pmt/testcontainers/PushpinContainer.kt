package io.github.mpecan.pmt.testcontainers

import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Testcontainer implementation for Pushpin proxy server.
 *
 * This container provides a fully-featured Pushpin server for integration testing,
 * with support for:
 * - HTTP/HTTPS proxying
 * - WebSocket connections
 * - Server-Sent Events (SSE)
 * - Long polling
 * - ZMQ and HTTP message publishing
 * - Custom routing configuration
 * - Debug logging and monitoring
 * * The container automatically:
 * - Generates pushpin.conf from the provided [PushpinConfiguration]
 * - Creates route files based on configured routes
 * - Exposes all necessary ports (HTTP, ZMQ publish, control, etc.)
 * - Enables host access for routing to test applications
 * - Provides verbose logging for debugging
 * * Example usage:
 * ```kotlin
 * val container = PushpinContainer()
 *     .withHostApplicationPort(8080)
 *     .withRoute("api/`*`", "host.testcontainers.internal:8080,over_http")
 *     .withConfiguration { copy(debug = true, logLevel = 5) }
 * * container.start()
 * * // Access the container
 * val httpUrl = container.getHttpUrl()
 * val publishPort = container.getPublishPort()
 * ```
 * * For easier configuration, use [PushpinContainerBuilder]:
 * ```kotlin
 * val container = PushpinContainerBuilder()
 *     .withPreset(PushpinPresets.webSocket())
 *     .withHostApplicationPort(8080)
 *     .build()
 * ```
 *
 * @param dockerImageName The Docker image to use for Pushpin (default: fanout/pushpin:1.40.1)
 * @param configuration The Pushpin configuration to use
 * @see PushpinConfiguration
 * @see PushpinContainerBuilder
 * @see PushpinPresets
 */
@Suppress("unused")
class PushpinContainer(
    dockerImageName: String = DEFAULT_IMAGE,
    private var configuration: PushpinConfiguration = PushpinConfiguration(),
) : GenericContainer<PushpinContainer>(DockerImageName.parse(dockerImageName)) {

    companion object {
        const val DEFAULT_IMAGE = "fanout/pushpin:1.40.1"

        // Default ports
        const val DEFAULT_HTTP_PORT = 7999
        const val DEFAULT_PUBLISH_PORT = 5560
        const val DEFAULT_HTTP_PUBLISH_PORT = 5561
        const val DEFAULT_SUB_PORT = 5562
        const val DEFAULT_CONTROL_PORT = 5563

        private val logger = LoggerFactory.getLogger(PushpinContainer::class.java)
    }

    init {
        configuration = configuration.copy(
            commandPort = DEFAULT_CONTROL_PORT,
            httpPort = DEFAULT_HTTP_PORT,
            pushInHttpPort = DEFAULT_HTTP_PUBLISH_PORT,
            pushInPort = DEFAULT_PUBLISH_PORT,
            pushInSubPort = DEFAULT_SUB_PORT,
        )
    }

    private var hostApplicationPort: Int = 8080
    private var routes: Map<String, String> = mapOf("*" to "host.testcontainers.internal:8080,over_http")
    private val exposedPorts = mutableSetOf<Int>()

    init {
        // Expose default ports
        withExposedPorts(
            configuration.httpPort,
            configuration.pushInHttpPort,
            extractPort(configuration.pushInSpec),
            extractPort(configuration.pushInSubSpec),
            extractPort(configuration.commandSpec),
        )

        // Enable host access for routing
        withAccessToHost(true)

        // Log container output
        withLogConsumer(Slf4jLogConsumer(logger))

        // Mount volume for logs
        withFileSystemBind("logs", "/var/log/pushpin", BindMode.READ_WRITE)
    }

    /**
     * Set the host application port that Pushpin will route to.
     */
    fun withHostApplicationPort(port: Int): PushpinContainer {
        this.hostApplicationPort = port
        return this
    }

    /**
     * Update the Pushpin configuration.
     */
    fun withConfiguration(configuration: PushpinConfiguration): PushpinContainer {
        this.configuration = configuration
        // Re-expose ports based on new configuration
        clearExposedPorts()
        withExposedPorts(
            configuration.httpPort,
            configuration.pushInHttpPort,
            configuration.pushInPort,
            configuration.pushInSubPort,
            configuration.commandPort,
        )
        return this
    }

    /**
     * Configure specific configuration properties using a builder pattern.
     */
    fun withConfiguration(configure: PushpinConfiguration.() -> PushpinConfiguration): PushpinContainer {
        this.configuration = configuration.configure()
        return this
    }

    /**
     * Set custom routes for Pushpin.
     *
     * @param routes Map of route patterns to targets (e.g., "*" to "localhost:8080,over_http")
     */
    fun withRoutes(routes: Map<String, String>): PushpinContainer {
        this.routes = routes
        return this
    }

    /**
     * Add a single route.
     */
    fun withRoute(pattern: String, target: String): PushpinContainer {
        this.routes = routes + (pattern to target)
        return this
    }

    /**
     * Use a simple route pointing to the host application.
     */
    fun withSimpleHostRoute(pattern: String = "*"): PushpinContainer {
        this.routes = mapOf(pattern to "host.testcontainers.internal:$hostApplicationPort,over_http")
        return this
    }

    override fun configure() {
        super.configure()
        // Expose host port for Pushpin to connect to
        Testcontainers.exposeHostPorts(hostApplicationPort)

        // Generate configuration and routes content
        val configContent = configuration.toConfigString()
        val routesContent = routes.entries.joinToString("\n") { (pattern, target) ->
            "$pattern $target"
        }

        // Set up the container with generated configuration
        withCopyToContainer(Transferable.of(configContent), "/etc/pushpin/pushpin.conf")
        withCopyToContainer(Transferable.of(routesContent), "/etc/pushpin/routes")
        logger.info("Pushpin routes:\n$routesContent")
        withCopyToContainer(
            Transferable.of(
                """#!/bin/sh
            echo '====== PUSHPIN CONFIG ======' >&2
            cat /etc/pushpin/pushpin.conf 
            echo '============================' >&2
            echo '====== ROUTES CONFIG ======' >&2
            cat /etc/pushpin/routes 
            echo '============================' >&2
            pushpin start --verbose
                """.trimIndent(),
                777,
            ),
            "/etc/pushpin/pushpin-start.sh",
        )

        // Start Pushpin with verbose logging
        withCommand("/etc/pushpin/pushpin-start.sh")
        // Wait for Pushpin to be ready
        waitingFor(
            Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(60)),
        )
    }

    override fun start() {
        super.start()
        logger.info("Pushpin container started with HTTP port: ${getHttpPort()}")
    }

    /**
     * Get the mapped HTTP port.
     */
    fun getHttpPort(): Int = getMappedPort(configuration.httpPort)

    /**
     * Get the mapped publish port (PULL socket).
     */
    fun getPublishPort(): Int = getMappedPort(extractPort(configuration.pushInSpec))

    /**
     * Get the mapped HTTP publish port.
     */
    fun getHttpPublishPort(): Int = getMappedPort(configuration.pushInHttpPort)

    /**
     * Get the mapped SUB port.
     */
    fun getSubPort(): Int = getMappedPort(extractPort(configuration.pushInSubSpec))

    /**
     * Get the mapped control port (REP socket).
     */
    fun getControlPort(): Int = getMappedPort(extractPort(configuration.commandSpec))

    /**
     * Get the base URL for HTTP connections to Pushpin.
     */
    fun getHttpUrl(): String = "http://$host:${getHttpPort()}"

    /**
     * Get the ZMQ publish URL for connecting to Pushpin's PULL socket.
     */
    fun getPublishUrl(): String = "tcp://$host:${getPublishPort()}"

    /**
     * Get the ZMQ control URL for connecting to Pushpin's REP socket.
     */
    fun getControlUrl(): String = "tcp://$host:${getControlPort()}"

    /**
     * Get the ZMQ SUB URL for subscribing to Pushpin's PUB socket.
     */
    fun getSubUrl(): String = "tcp://$host:${getSubPort()}"

    /**
     * Get the HTTP publish URL for sending messages via HTTP.
     */
    fun getHttpPublishUrl(): String = "http://$host:${getHttpPublishPort()}"

    /**
     * Extract port number from a ZMQ spec string.
     */
    private fun extractPort(spec: String): Int {
        // Handle specs like "tcp://*:5560" or "tcp://0.0.0.0:5560"
        val portMatch = Regex(":([0-9]+)").find(spec)
        return portMatch?.groupValues?.get(1)?.toInt()
            ?: throw IllegalArgumentException("Cannot extract port from spec: $spec")
    }

    /**
     * Clear exposed ports (used when reconfiguring).
     */
    private fun clearExposedPorts() {
        exposedPorts.clear()
    }
}
