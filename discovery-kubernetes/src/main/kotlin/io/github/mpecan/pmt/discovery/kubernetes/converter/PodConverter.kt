package io.github.mpecan.pmt.discovery.kubernetes.converter

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.github.mpecan.pmt.model.PushpinServer
import io.kubernetes.client.openapi.models.V1Pod
import org.slf4j.LoggerFactory

/**
 * Interface for converting Kubernetes pods to PushpinServer instances.
 */
interface PodConverter {
    /**
     * Convert a Kubernetes pod to a PushpinServer instance.
     *
     * @param pod The Kubernetes pod.
     * @param properties The Kubernetes discovery properties.
     * @return A PushpinServer instance.
     */
    fun toPushpinServer(pod: V1Pod, properties: KubernetesDiscoveryProperties): PushpinServer
}

/**
 * Default implementation of PodConverter that converts Kubernetes pods to PushpinServer instances.
 */
class DefaultPodConverter : PodConverter {
    private val logger = LoggerFactory.getLogger(DefaultPodConverter::class.java)

    override fun toPushpinServer(pod: V1Pod, properties: KubernetesDiscoveryProperties): PushpinServer {
        // Get pod information
        val podName = pod.metadata?.name ?: "unknown-pod"
        val namespace = pod.metadata?.namespace ?: "default"
        val hostIp = pod.status?.hostIP
        val podIp = pod.status?.podIP
        
        // Get appropriate IP address based on configuration
        val host = when {
            // If using NodePort, use the host IP
            properties.useNodePort && hostIp != null -> hostIp
            // Otherwise use pod IP
            podIp != null -> podIp
            // Fallback (should not happen with healthy pods)
            else -> {
                logger.warn("Pod $podName has no IP address, using localhost")
                "localhost"
            }
        }
        
        // Extract custom port values from annotations if present
        val annotations = pod.metadata?.annotations ?: emptyMap()
        val httpPort = annotations["pushpin.io/http-port"]?.toIntOrNull() ?: properties.port
        val controlPort = annotations["pushpin.io/control-port"]?.toIntOrNull() ?: properties.controlPort
        val publishPort = annotations["pushpin.io/publish-port"]?.toIntOrNull() ?: properties.publishPort
        
        // Generate a unique server ID
        val serverId = "$namespace-$podName"
        
        return PushpinServer(
            id = serverId,
            host = host,
            port = httpPort,
            controlPort = controlPort,
            publishPort = publishPort,
            healthCheckPath = properties.healthCheckPath
        )
    }
}