package io.github.mpecan.pmt.testcontainers

import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Testcontainer for Pushpin server.
 *
 * This container runs a Pushpin server with the necessary configuration for integration testing.
 */
class PushpinContainer(
    dockerImageName: String = "fanout/pushpin:1.40.1"
) : GenericContainer<PushpinContainer>(DockerImageName.parse(dockerImageName)) {

    companion object {
        const val HTTP_PORT = 7999
        const val PUBLISH_PORT = 5560
        const val XPUB_PORT = 5561
        const val ROUTER_PORT = 5562
        const val CONTROL_PORT = 5563

        private val logger = LoggerFactory.getLogger(PushpinContainer::class.java)
    }

    private var hostPort: Int = 8080

    fun withHostPort(port: Int): PushpinContainer {
        this.hostPort = port
        return this
    }

    init {
        // Expose all required ports
        withExposedPorts(HTTP_PORT, PUBLISH_PORT, XPUB_PORT, ROUTER_PORT, CONTROL_PORT)
        withAccessToHost(true)

        // Log container output
        withLogConsumer(Slf4jLogConsumer(logger))
    }

    override fun start() {
        // Set environment variables dynamically right before container starts
        withEnv("HTTP_PORT", HTTP_PORT.toString())
        withFileSystemBind("logs", "/var/log/pushpin", BindMode.READ_WRITE)
        Testcontainers.exposeHostPorts(hostPort)
        // Create configuration with dynamic port - simplified for testability
        withCommand(
            "sh", "-c",
            // Create routes file pointing to the host application
            "echo '* host.testcontainers.internal:$hostPort,over_http' > /etc/pushpin/routes && " +
                    // Start modifying the config file by ensuring these port sections are set correctly
                    "sed -i 's/http_port=.*/http_port=$HTTP_PORT/' /etc/pushpin/pushpin.conf && " +

                    // ZMQ publish socket config - ensure socket binding works correctly
                    "sed -i 's/push_in_spec=.*/push_in_spec=tcp:\\/\\/*:$PUBLISH_PORT/' /etc/pushpin/pushpin.conf && " +
                    "sed -i 's/push_in_http_port=.*/push_in_http_port=$XPUB_PORT/' /etc/pushpin/pushpin.conf && " +
                    "sed -i 's/command_spec=.*/command_spec=tcp:\\/\\/*:$CONTROL_PORT/' /etc/pushpin/pushpin.conf && " +
                    // Enable ZMQ for message handling
                    "sed -i 's/zmq_publish=.*/zmq_publish=true/' /etc/pushpin/pushpin.conf && " +
                    // Configure handler section for ZMQ
                    "echo '[handler]' >> /etc/pushpin/pushpin.conf && " +
                    "echo 'm2a_in_specs=tcp://*:$PUBLISH_PORT' >> /etc/pushpin/pushpin.conf && " +
                    "echo 'm2a_in_stream=true' >> /etc/pushpin/pushpin.conf && " +
                    "echo 'm2a_in_http=true' >> /etc/pushpin/pushpin.conf && " +

                    // Set log level to debug for better diagnostics
                    "sed -i 's/log_level=.*/log_level=3/' /etc/pushpin/pushpin.conf && " +

                    // Set simple server ID
                    "sed -i 's/id=.*/id=pushpin-test/' /etc/pushpin/pushpin.conf && " +

                    // Print configuration for debugging
                    "echo '====== PUSHPIN CONFIG ======' && " +
                    "cat /etc/pushpin/pushpin.conf && " +
                    "echo '====== ROUTES CONFIG ======' && " +
                    "cat /etc/pushpin/routes && " +

                    // Start Pushpin with verbose logging
                    "pushpin --verbose"
        )

        // Use a wait strategy that checks both HTTP and control ports
        waitingFor(
            Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(60))
        )

        super.start()
    }

    /**
     * Gets the mapped HTTP port.
     */
    fun getHttpPort(): Int = getMappedPort(HTTP_PORT)

    /**
     * Gets the mapped publish port.
     */
    fun getPublishPort(): Int = getMappedPort(PUBLISH_PORT)

    /**
     * Gets the mapped control port.
     */
    fun getControlPort(): Int = getMappedPort(CONTROL_PORT)

    /**
     * Gets the mapped XPUB port.
     */
    fun getHttpPublishPort(): Int = getMappedPort(XPUB_PORT)

    /**
     * Gets the base URL for HTTP requests.
     */
    fun getBaseUrl(): String = "http://${host}:${getHttpPort()}"

    /**
     * Gets the control URL for publishing messages.
     */
    fun getControlUrl(): String = "http://${host}:${getControlPort()}"

    /**
     * Gets the publish URL for ZMQ connections.
     */
    fun getPublishUrl(): String = "tcp://${host}:${getPublishPort()}"
}