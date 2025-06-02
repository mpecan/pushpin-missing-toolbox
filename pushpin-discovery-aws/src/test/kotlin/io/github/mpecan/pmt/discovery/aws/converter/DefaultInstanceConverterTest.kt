package io.github.mpecan.pmt.discovery.aws.converter

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.Tag
import kotlin.test.assertEquals

class DefaultInstanceConverterTest {
    private lateinit var converter: DefaultInstanceConverter
    private lateinit var properties: AwsDiscoveryProperties

    @BeforeEach
    fun setUp() {
        converter = DefaultInstanceConverter()
        properties =
            AwsDiscoveryProperties(
                enabled = true,
                port = 7999,
                controlPort = 5564,
                publishPort = 5560,
                healthCheckPath = "/test/health",
                privateIp = true,
            )
    }

    @Test
    fun `should use private IP when privateIp is true`() {
        // Create an instance with both private and public IPs
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .publicIpAddress("54.0.0.1")
                .build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, properties)

        // Verify that private IP is used
        assertEquals("10.0.0.1", server.host)
        assertEquals(7999, server.port)
        assertEquals(5564, server.controlPort)
        assertEquals(5560, server.publishPort)
        assertEquals("/test/health", server.healthCheckPath)
        assertEquals("i-12345", server.id)
    }

    @Test
    fun `should use public IP when privateIp is false`() {
        // Set privateIp to false in properties
        val publicIpProperties = properties.copy(privateIp = false)

        // Create an instance with both private and public IPs
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .publicIpAddress("54.0.0.1")
                .build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, publicIpProperties)

        // Verify that public IP is used
        assertEquals("54.0.0.1", server.host)
    }

    @Test
    fun `should fall back to private IP when public IP is null`() {
        // Set privateIp to false in properties
        val publicIpProperties = properties.copy(privateIp = false)

        // Create an instance with only private IP
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .publicIpAddress(null)
                .build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, publicIpProperties)

        // Verify that private IP is used as fallback
        assertEquals("10.0.0.1", server.host)
    }

    @Test
    fun `should use Name tag for server ID if available`() {
        // Create an instance with a Name tag
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .tags(
                    Tag
                        .builder()
                        .key("Name")
                        .value("pushpin-server-1")
                        .build(),
                ).build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, properties)

        // Verify that Name tag is used for ID
        assertEquals("pushpin-server-1", server.id)
    }

    @Test
    fun `should use instance ID for server ID if Name tag is not available`() {
        // Create an instance without a Name tag
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .tags(
                    Tag
                        .builder()
                        .key("OtherTag")
                        .value("other-value")
                        .build(),
                ).build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, properties)

        // Verify that instance ID is used for ID
        assertEquals("i-12345", server.id)
    }

    @Test
    fun `should use configured port values`() {
        // Create properties with custom port values
        val customPortProperties =
            properties.copy(
                port = 8000,
                controlPort = 6000,
                publishPort = 7000,
                healthCheckPath = "/custom/health",
            )

        // Create an instance
        val instance =
            Instance
                .builder()
                .instanceId("i-12345")
                .privateIpAddress("10.0.0.1")
                .build()

        // Convert to PushpinServer
        val server = converter.toPushpinServer(instance, customPortProperties)

        // Verify that custom port values are used
        assertEquals(8000, server.port)
        assertEquals(6000, server.controlPort)
        assertEquals(7000, server.publishPort)
        assertEquals("/custom/health", server.healthCheckPath)
    }
}
