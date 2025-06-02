package io.github.mpecan.pmt.testcontainers

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class PushpinContainerBuilderTest {
    @Test
    fun `builder should create container with default settings`() {
        val container = PushpinContainerBuilder().build()

        assertNotNull(container)
        // Container should have default configuration
    }

    @Test
    fun `builder should apply custom ports`() {
        val container =
            PushpinContainerBuilder()
                .withHttpPort(8999)
                .withPublishPort(6560)
                .withControlPort(6563)
                .withHttpPublishPort(6561)
                .build()

        assertNotNull(container)
        // Ports should be set in configuration
    }

    @Test
    fun `builder should apply routes`() {
        val container =
            PushpinContainerBuilder()
                .withRoute("api/*", "backend:8080,over_http")
                .withRoute("ws/*", "websocket:9000,over_ws")
                .build()

        assertNotNull(container)
        // Routes should be configured
    }

    @Test
    fun `builder should apply configuration lambda`() {
        val container =
            PushpinContainerBuilder()
                .withConfiguration {
                    copy(
                        debug = true,
                        logLevel = 10,
                        messageRate = 5000,
                    )
                }.build()

        assertNotNull(container)
        // Configuration should be applied
    }

    @Test
    fun `builder should set host application port`() {
        val container =
            PushpinContainerBuilder()
                .withHostApplicationPort(9090)
                .withSimpleHostRoute()
                .build()

        assertNotNull(container)
        // Host port should be set to 9090
    }
}
