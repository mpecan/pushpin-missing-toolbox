package io.github.mpecan.pmt.discovery.kubernetes.health

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodCondition
import io.kubernetes.client.openapi.models.V1PodStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPodHealthCheckerTest {

    private val healthChecker = DefaultPodHealthChecker()
    private val properties = KubernetesDiscoveryProperties(healthCheckEnabled = true)
    private val propertiesWithoutHealthCheck = KubernetesDiscoveryProperties(healthCheckEnabled = false)

    @Test
    fun `should return false for non-running pod`() {
        // Given
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod"))
            .status(V1PodStatus().phase("Pending"))

        // When
        val isHealthy = healthChecker.isHealthy(pod, properties)

        // Then
        assertFalse(isHealthy)
    }

    @Test
    fun `should return true for running pod with ready condition`() {
        // Given
        val readyCondition = V1PodCondition()
            .type("Ready")
            .status("True")
        
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod"))
            .status(V1PodStatus()
                .phase("Running")
                .conditions(listOf(readyCondition)))

        // When
        val isHealthy = healthChecker.isHealthy(pod, properties)

        // Then
        assertTrue(isHealthy)
    }

    @Test
    fun `should return false for running pod without ready condition`() {
        // Given
        val readyCondition = V1PodCondition()
            .type("Ready")
            .status("False")
        
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod"))
            .status(V1PodStatus()
                .phase("Running")
                .conditions(listOf(readyCondition)))

        // When
        val isHealthy = healthChecker.isHealthy(pod, properties)

        // Then
        assertFalse(isHealthy)
    }

    @Test
    fun `should return true for running pod when health checks are disabled`() {
        // Given
        // The ready condition is False, but health checks are disabled
        val readyCondition = V1PodCondition()
            .type("Ready")
            .status("False")
        
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod"))
            .status(V1PodStatus()
                .phase("Running")
                .conditions(listOf(readyCondition)))

        // When
        val isHealthy = healthChecker.isHealthy(pod, propertiesWithoutHealthCheck)

        // Then
        assertTrue(isHealthy)
    }

    @Test
    fun `should return false for non-running pod when health checks are disabled`() {
        // Given
        val pod = V1Pod()
            .metadata(V1ObjectMeta().name("test-pod"))
            .status(V1PodStatus().phase("Failed"))

        // When
        val isHealthy = healthChecker.isHealthy(pod, propertiesWithoutHealthCheck)

        // Then
        assertFalse(isHealthy)
    }
}