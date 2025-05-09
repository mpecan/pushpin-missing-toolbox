package io.github.mpecan.pmt.discovery.kubernetes

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Kubernetes-based discovery.
 *
 * @property enabled Whether Kubernetes-based discovery is enabled
 * @property namespace The Kubernetes namespace to search for Pushpin pods (null means all namespaces)
 * @property labelSelector Label selector to filter pods (e.g. "app=pushpin")
 * @property fieldSelector Field selector to filter pods (e.g. "status.phase=Running")
 * @property kubeConfigPath Path to the kubeconfig file (null for in-cluster configuration)
 * @property refreshCacheSeconds How often to refresh the pods cache (in seconds)
 * @property healthCheckEnabled Whether to check pod health status
 * @property port Default HTTP port for Pushpin servers
 * @property controlPort Default control port for Pushpin servers
 * @property publishPort Default publish port for Pushpin servers
 * @property healthCheckPath Default health check path for Pushpin servers
 * @property useNodePort Whether to use NodePort for accessing services (if false, use pod IP directly)
 * @property useService Whether to discover services instead of pods
 * @property serviceName Name of the service to discover (only used when useService is true)
 */
@ConfigurationProperties(prefix = "pushpin.discovery.kubernetes")
data class KubernetesDiscoveryProperties(
    val enabled: Boolean = false,
    val namespace: String? = null,
    val labelSelector: String? = "app=pushpin",
    val fieldSelector: String? = "status.phase=Running",
    val kubeConfigPath: String? = null,
    val refreshCacheSeconds: Int = 30,
    val healthCheckEnabled: Boolean = true,
    val port: Int = 7999,
    val controlPort: Int = 5564,
    val publishPort: Int = 5560,
    val healthCheckPath: String = "/api/health/check",
    val useNodePort: Boolean = false,
    val useService: Boolean = false,
    val serviceName: String = "pushpin"
)