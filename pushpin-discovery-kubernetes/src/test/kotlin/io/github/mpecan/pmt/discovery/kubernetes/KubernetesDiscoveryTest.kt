package io.github.mpecan.pmt.discovery.kubernetes

import io.github.mpecan.pmt.discovery.kubernetes.converter.PodConverter
import io.github.mpecan.pmt.discovery.kubernetes.health.PodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.pods.KubernetesPodProvider
import io.github.mpecan.pmt.model.PushpinServer
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier
import java.time.Duration

class KubernetesDiscoveryTest {
    private val podProvider: KubernetesPodProvider = mock()
    private val podHealthChecker: PodHealthChecker = mock()
    private val podConverter: PodConverter = mock()
    private val properties =
        KubernetesDiscoveryProperties(
            enabled = true,
            namespace = "test-namespace",
            labelSelector = "app=pushpin",
            refreshCacheSeconds = 10,
        )

    @Test
    fun `should return empty flux when disabled`() {
        // Given
        val disabledProperties = KubernetesDiscoveryProperties(enabled = false)
        val discovery =
            KubernetesDiscovery(
                properties = disabledProperties,
                podProvider = podProvider,
                podHealthChecker = podHealthChecker,
                podConverter = podConverter,
            )

        // When
        val result = discovery.discoverServers()

        // Then
        StepVerifier
            .create(result)
            .expectComplete()
            .verify(Duration.ofSeconds(1))
    }

    @Test
    fun `should discover servers from kubernetes pods`() {
        // Given
        val discovery =
            KubernetesDiscovery(
                properties = properties,
                podProvider = podProvider,
                podHealthChecker = podHealthChecker,
                podConverter = podConverter,
            )

        val pod1 = createPod("pod-1", "192.168.1.1")
        val pod2 = createPod("pod-2", "192.168.1.2")
        val pods = listOf(pod1, pod2)

        val server1 = PushpinServer(id = "pod-1", host = "192.168.1.1", port = 7999)
        val server2 = PushpinServer(id = "pod-2", host = "192.168.1.2", port = 7999)

        // Mock behavior
        whenever(podProvider.getPods(any())).thenReturn(pods)
        whenever(podHealthChecker.isHealthy(pod1, properties)).thenReturn(true)
        whenever(podHealthChecker.isHealthy(pod2, properties)).thenReturn(true)
        whenever(podConverter.toPushpinServer(pod1, properties)).thenReturn(server1)
        whenever(podConverter.toPushpinServer(pod2, properties)).thenReturn(server2)

        // When
        val result = discovery.discoverServers()

        // Then
        StepVerifier
            .create(result)
            .expectNextMatches { server -> server.id == "pod-1" || server.id == "pod-2" }
            .expectNextMatches { server -> server.id == "pod-1" || server.id == "pod-2" }
            .expectComplete()
            .verify(Duration.ofSeconds(1))
    }

    @Test
    fun `should filter unhealthy pods`() {
        // Given
        val discovery =
            KubernetesDiscovery(
                properties = properties,
                podProvider = podProvider,
                podHealthChecker = podHealthChecker,
                podConverter = podConverter,
            )

        val pod1 = createPod("pod-1", "192.168.1.1")
        val pod2 = createPod("pod-2", "192.168.1.2")
        val pods = listOf(pod1, pod2)

        val server1 = PushpinServer(id = "pod-1", host = "192.168.1.1", port = 7999)

        // Mock behavior
        whenever(podProvider.getPods(any())).thenReturn(pods)
        whenever(podHealthChecker.isHealthy(pod1, properties)).thenReturn(true)
        whenever(podHealthChecker.isHealthy(pod2, properties)).thenReturn(false) // pod2 is unhealthy
        whenever(podConverter.toPushpinServer(pod1, properties)).thenReturn(server1)

        // When
        val result = discovery.discoverServers()

        // Then
        StepVerifier
            .create(result)
            .expectNextMatches { server -> server.id == "pod-1" }
            .expectComplete()
            .verify(Duration.ofSeconds(1))
    }

    private fun createPod(
        name: String,
        podIp: String,
    ): V1Pod =
        V1Pod()
            .metadata(V1ObjectMeta().name(name))
            .status(V1PodStatus().podIP(podIp))
}
