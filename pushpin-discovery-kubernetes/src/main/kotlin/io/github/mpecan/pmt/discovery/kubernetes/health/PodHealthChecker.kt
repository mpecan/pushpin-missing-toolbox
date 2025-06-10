package io.github.mpecan.pmt.discovery.kubernetes.health

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.kubernetes.client.openapi.models.V1Pod
import org.slf4j.LoggerFactory

/**
 * Interface for checking the health of Kubernetes pods.
 */
fun interface PodHealthChecker {
    /**
     * Check if a Kubernetes pod is healthy.
     *
     * @param pod The Kubernetes pod.
     * @param properties The Kubernetes discovery properties.
     * @return True if the pod is healthy, false otherwise.
     */
    fun isHealthy(
        pod: V1Pod,
        properties: KubernetesDiscoveryProperties,
    ): Boolean
}

/**
 * Default implementation of PodHealthChecker that checks if a Kubernetes pod is running and ready.
 */
class DefaultPodHealthChecker : PodHealthChecker {
    private val logger = LoggerFactory.getLogger(DefaultPodHealthChecker::class.java)

    override fun isHealthy(
        pod: V1Pod,
        properties: KubernetesDiscoveryProperties,
    ): Boolean {
        val podName = pod.metadata?.name ?: "unknown-pod"

        // If health checks are disabled, just check if the pod is running
        if (!properties.healthCheckEnabled) {
            return isPodRunning(pod)
        }

        // Check if pod is running
        if (!isPodRunning(pod)) {
            logger.debug("Pod $podName is not running")
            return false
        }

        // Check if pod is ready
        if (!isPodReady(pod)) {
            logger.debug("Pod $podName is running but not ready")
            return false
        }

        return true
    }

    /**
     * Check if a pod is in the "Running" phase.
     */
    private fun isPodRunning(pod: V1Pod): Boolean {
        val phase = pod.status?.phase
        return phase == "Running"
    }

    /**
     * Check if a pod is ready based on its ready condition.
     */
    private fun isPodReady(pod: V1Pod): Boolean {
        val conditions = pod.status?.conditions ?: return false

        // Find the "Ready" condition
        val readyCondition =
            conditions.find { condition ->
                condition.type == "Ready"
            }

        // Check if the "Ready" condition is true
        return readyCondition?.status == "True"
    }
}
