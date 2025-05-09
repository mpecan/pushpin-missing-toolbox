package io.github.mpecan.pmt.discovery.kubernetes.converter

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultPodConverterTest {

    private val converter = DefaultPodConverter()
    private val properties = KubernetesDiscoveryProperties(
        port = 7999,
        controlPort = 5564,
        publishPort = 5560,
        healthCheckPath = "/test/health",
    )

    @Test
    fun `should convert pod to pushpin server using pod IP`() {
        // Given
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod").namespace("test-namespace"))
            .status(V1PodStatus().podIP("192.168.1.1").hostIP("10.0.0.1"))

        // When
        val server = converter.toPushpinServer(pod, properties)

        // Then
        assertEquals("test-namespace-test-pod", server.id)
        assertEquals("192.168.1.1", server.host)
        assertEquals(7999, server.port)
        assertEquals(5564, server.controlPort)
        assertEquals(5560, server.publishPort)
        assertEquals("/test/health", server.healthCheckPath)
        assertEquals("http://192.168.1.1:7999", server.getBaseUrl())
    }

    @Test
    fun `should convert pod to pushpin server using host IP when nodePort is true`() {
        // Given
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod").namespace("test-namespace"))
            .status(V1PodStatus().podIP("192.168.1.1").hostIP("10.0.0.1"))

        val nodePortProperties = KubernetesDiscoveryProperties(
            useNodePort = true,
            port = 30999, // NodePort is typically in the 30000-32767 range
            controlPort = 30564,
            publishPort = 30560,
        )

        // When
        val server = converter.toPushpinServer(pod, nodePortProperties)

        // Then
        assertEquals("test-namespace-test-pod", server.id)
        assertEquals("10.0.0.1", server.host) // Should use host IP instead of pod IP
        assertEquals(30999, server.port)
        assertEquals(30564, server.controlPort)
        assertEquals(30560, server.publishPort)
    }

    @Test
    fun `should use custom port values from annotations`() {
        // Given
        val annotations = mapOf(
            "pushpin.io/http-port" to "8080",
            "pushpin.io/control-port" to "6000",
            "pushpin.io/publish-port" to "6001",
        )

        val pod = V1Pod()
            .metadata(
                V1ObjectMeta()
                    .name("test-pod")
                    .namespace("test-namespace")
                    .annotations(annotations),
            )
            .status(V1PodStatus().podIP("192.168.1.1"))

        // When
        val server = converter.toPushpinServer(pod, properties)

        // Then
        assertEquals("192.168.1.1", server.host)
        assertEquals(8080, server.port) // From annotation
        assertEquals(6000, server.controlPort) // From annotation
        assertEquals(6001, server.publishPort) // From annotation
    }

    @Test
    fun `should fallback to localhost if no IP is available`() {
        // Given
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod").namespace("test-namespace"))
            .status(V1PodStatus()) // No IP addresses

        // When
        val server = converter.toPushpinServer(pod, properties)

        // Then
        assertEquals("localhost", server.host)
    }
}
