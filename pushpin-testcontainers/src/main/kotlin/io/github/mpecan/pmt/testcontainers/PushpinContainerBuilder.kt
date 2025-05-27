package io.github.mpecan.pmt.testcontainers

import io.github.mpecan.pmt.testcontainers.PushpinContainer.Companion.DEFAULT_CONTROL_PORT
import io.github.mpecan.pmt.testcontainers.PushpinContainer.Companion.DEFAULT_HTTP_PORT
import io.github.mpecan.pmt.testcontainers.PushpinContainer.Companion.DEFAULT_HTTP_PUBLISH_PORT
import io.github.mpecan.pmt.testcontainers.PushpinContainer.Companion.DEFAULT_PUBLISH_PORT
import io.github.mpecan.pmt.testcontainers.PushpinContainer.Companion.DEFAULT_SUB_PORT

/**
 * Builder for creating PushpinContainer instances with a fluent API.
 * * This builder provides a convenient way to configure and create Pushpin containers
 * for integration testing. It allows you to:
 * - Configure network ports (HTTP, ZMQ publish, control, etc.)
 * - Set up routing rules for request forwarding
 * - Apply configuration presets for common scenarios
 * - Enable debug mode and adjust logging levels
 * - Customize the Docker image used
 * * The builder starts with sensible defaults and allows incremental customization.
 * Call [build] to create the configured container instance.
 *
 * Example usage:
 * ```kotlin
 * val container = PushpinContainerBuilder()
 *     .withHttpPort(8999)
 *     .withDebugMode(true)
 *     .withLogLevel(5)
 *     .withHostApplicationPort(8080)
 *     .withRoute("api/`*`", "localhost:8080,over_http")
 *     .build()
 * ```
 * * For common configurations, consider using presets:
 * ```kotlin
 * val container = PushpinContainerBuilder()
 *     .withPreset(PushpinPresets.webSocket())
 *     .withHostApplicationPort(8080)
 *     .build()
 * ```
 * * @see PushpinContainer
 * @see PushpinPresets
 */
@Suppress("unused")
class PushpinContainerBuilder {
    private var dockerImage: String = PushpinContainer.DEFAULT_IMAGE
    private var configuration: PushpinConfiguration = PushpinConfiguration(
        commandPort = DEFAULT_CONTROL_PORT,
        httpPort = DEFAULT_HTTP_PORT,
        pushInHttpPort = DEFAULT_HTTP_PUBLISH_PORT,
        pushInPort = DEFAULT_PUBLISH_PORT,
        pushInSubPort = DEFAULT_SUB_PORT,
    )
    private var hostApplicationPort: Int = 8080
    private val routes = mutableMapOf<String, String>()

    /**
     * Set the Docker image to use.
     */
    fun withDockerImage(image: String): PushpinContainerBuilder {
        this.dockerImage = image
        return this
    }

    /**
     * Set the HTTP port for Pushpin.
     */
    fun withHttpPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(httpPort = port)
        return this
    }

    /**
     * Set the HTTPS port for Pushpin.
     */
    fun withHttpsPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(httpsPort = port)
        return this
    }

    /**
     * Enable or disable debug mode.
     */
    fun withDebugMode(enabled: Boolean): PushpinContainerBuilder {
        configuration = configuration.copy(debug = enabled)
        return this
    }

    /**
     * Set the log level (2 = info, >2 = verbose).
     */
    fun withLogLevel(level: Int): PushpinContainerBuilder {
        configuration = configuration.copy(logLevel = level)
        return this
    }

    /**
     * Set the ZMQ publish port.
     */
    fun withPublishPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(pushInPort = port)
        return this
    }

    /**
     * Set the HTTP publish port.
     */
    fun withHttpPublishPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(pushInHttpPort = port)
        return this
    }

    /**
     * Set the ZMQ SUB port.
     */
    fun withSubPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(pushInSubPort = port)
        return this
    }

    /**
     * Set the control port.
     */
    fun withControlPort(port: Int): PushpinContainerBuilder {
        configuration = configuration.copy(commandPort = port)
        return this
    }

    /**
     * Set the host application port that Pushpin will route to.
     */
    fun withHostApplicationPort(port: Int): PushpinContainerBuilder {
        this.hostApplicationPort = port
        return this
    }

    /**
     * Add a route pattern.
     */
    fun withRoute(pattern: String, target: String): PushpinContainerBuilder {
        routes[pattern] = target
        return this
    }

    /**
     * Add a simple route to the host application.
     */
    fun withSimpleHostRoute(pattern: String = "*"): PushpinContainerBuilder {
        routes[pattern] = "host.testcontainers.internal:$hostApplicationPort,over_http"
        return this
    }

    /**
     * Enable CORS handling.
     */
    fun withCorsEnabled(enabled: Boolean = true): PushpinContainerBuilder {
        configuration = configuration.copy(autoCrossOrigin = enabled)
        return this
    }

    /**
     * Set the signature key for signed requests.
     */
    fun withSignatureKey(key: String): PushpinContainerBuilder {
        configuration = configuration.copy(sigKey = key)
        return this
    }

    /**
     * Enable compression.
     */
    fun withCompression(enabled: Boolean = true): PushpinContainerBuilder {
        configuration = configuration.copy(allowCompression = enabled)
        return this
    }

    /**
     * Set max client connections.
     */
    fun withMaxConnections(max: Int): PushpinContainerBuilder {
        configuration = configuration.copy(clientMaxConn = max)
        return this
    }

    /**
     * Set message rate limit.
     */
    fun withMessageRate(rate: Int): PushpinContainerBuilder {
        configuration = configuration.copy(messageRate = rate)
        return this
    }

    /**
     * Configure with a custom configuration object.
     */
    fun withConfiguration(config: PushpinConfiguration): PushpinContainerBuilder {
        this.configuration = config
        return this
    }

    /**
     * Configure using a lambda.
     */
    fun withConfiguration(configure: PushpinConfiguration.() -> PushpinConfiguration): PushpinContainerBuilder {
        this.configuration = configuration.configure()
        return this
    }

    /**
     * Build the PushpinContainer.
     */
    fun build(): PushpinContainer {
        val container = PushpinContainer(dockerImage, configuration)
            .withHostApplicationPort(hostApplicationPort)

        // Add routes if any were specified
        if (routes.isNotEmpty()) {
            container.withRoutes(routes)
        } else {
            // Default route if none specified
            container.withSimpleHostRoute()
        }

        return container
    }
}
