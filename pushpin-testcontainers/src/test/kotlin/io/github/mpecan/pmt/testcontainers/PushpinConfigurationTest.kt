package io.github.mpecan.pmt.testcontainers

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class PushpinConfigurationTest {

    @Test
    fun `default configuration should generate valid config file`() {
        val config = PushpinConfiguration()
        val configString = config.toConfigString()

        // Check key sections exist
        assertTrue(configString.contains("[global]"))
        assertTrue(configString.contains("[runner]"))
        assertTrue(configString.contains("[proxy]"))
        assertTrue(configString.contains("[handler]"))

        // Check default values
        assertTrue(configString.contains("http_port=7999"))
        assertTrue(configString.contains("push_in_spec=tcp://*:5560"))
        assertTrue(configString.contains("command_spec=tcp://*:5563"))
        assertTrue(configString.contains("log_level=5"))
    }

    @Test
    fun `custom ports should be reflected in config`() {
        val config = PushpinConfiguration(
            httpPort = 8999,
            pushInSpec = "tcp://*:6560",
            commandSpec = "tcp://*:6563",
            pushInHttpPort = 6561,
        )
        val configString = config.toConfigString()

        assertTrue(configString.contains("http_port=8999"))
        assertTrue(configString.contains("push_in_spec=tcp://*:6560"))
        assertTrue(configString.contains("command_spec=tcp://*:6563"))
        assertTrue(configString.contains("push_in_http_port=6561"))
    }

    @Test
    fun `optional fields should only appear when set`() {
        val configWithHttps = PushpinConfiguration(httpsPort = 8443)
        val configWithoutHttps = PushpinConfiguration(httpsPort = null)

        assertTrue(configWithHttps.toConfigString().contains("https_ports=8443"))
        assertTrue(!configWithoutHttps.toConfigString().contains("https_ports"))
    }

    @Test
    fun `debug and logging settings should be configurable`() {
        val config = PushpinConfiguration(
            debug = true,
            logLevel = 10,
            logFrom = true,
            logUserAgent = true,
        )
        val configString = config.toConfigString()

        assertTrue(configString.contains("debug=true"))
        assertTrue(configString.contains("log_level=10"))
        assertTrue(configString.contains("log_from=true"))
        assertTrue(configString.contains("log_user_agent=true"))
    }
}

