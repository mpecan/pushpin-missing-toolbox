package io.github.mpecan.pmt.discovery.kubernetes.pods

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.github.mpecan.pmt.discovery.kubernetes.clients.KubernetesClientProvider
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.models.V1Pod
import org.slf4j.LoggerFactory

/**
 * Interface for providing Kubernetes pods.
 */
interface KubernetesPodProvider {
    /**
     * Get Kubernetes pods based on the provided properties.
     *
     * @param properties The Kubernetes discovery properties.
     * @return A list of Kubernetes pods.
     */
    fun getPods(properties: KubernetesDiscoveryProperties): List<V1Pod>
}

/**
 * Default implementation of KubernetesPodProvider that discovers pods from Kubernetes API.
 */
class DefaultKubernetesPodProvider(
    private val clientProvider: KubernetesClientProvider = KubernetesClientProvider(),
) : KubernetesPodProvider {
    private val logger = LoggerFactory.getLogger(DefaultKubernetesPodProvider::class.java)

    override fun getPods(properties: KubernetesDiscoveryProperties): List<V1Pod> {
        val pods = mutableListOf<V1Pod>()

        try {
            val coreApi = clientProvider.getCoreApi(properties)

            // If we're using a service, get pods via the service selector
            if (properties.useService) {
                pods.addAll(getPodsFromService(properties, coreApi))
            } else {
                // Otherwise get pods directly using label selector
                pods.addAll(getPodsDirectly(properties, coreApi))
            }

            logger.debug("Discovered ${pods.size} pods")
        } catch (e: Exception) {
            logger.error("Error discovering Kubernetes pods: ${e.message}", e)
        }

        return pods
    }

    /**
     * Get pods by directly querying with label selector.
     */
    private fun getPodsDirectly(
        properties: KubernetesDiscoveryProperties,
        coreApi: io.kubernetes.client.openapi.apis.CoreV1Api,
    ): List<V1Pod> {
        val pods = mutableListOf<V1Pod>()

        try {
            val podList = if (properties.namespace != null) {
                // Get pods in specific namespace using builder pattern
                coreApi.listNamespacedPod(properties.namespace)
                    .apply {
                        properties.fieldSelector?.let { fieldSelector(it) }
                        properties.labelSelector?.let { labelSelector(it) }
                    }
                    .execute()
            } else {
                // Get pods across all namespaces using builder pattern
                coreApi.listPodForAllNamespaces()
                    .apply {
                        properties.fieldSelector?.let { fieldSelector(it) }
                        properties.labelSelector?.let { labelSelector(it) }
                    }
                    .execute()
            }

            // Add all items from the pod list to our result list
            podList.items?.let { pods.addAll(it) }
        } catch (e: ApiException) {
            logger.error("Failed to list pods: ${e.responseBody}", e)
        }

        return pods
    }

    /**
     * Get pods by first finding the service and then its associated pods.
     */
    private fun getPodsFromService(
        properties: KubernetesDiscoveryProperties,
        coreApi: io.kubernetes.client.openapi.apis.CoreV1Api,
    ): List<V1Pod> {
        val pods = mutableListOf<V1Pod>()

        try {
            // Get the namespace to use
            val namespace = properties.namespace ?: "default"

            // Get the service using builder pattern
            val service = coreApi.readNamespacedService(properties.serviceName, namespace)
                .execute()

            // Extract the selector from the service
            val selector = service.spec?.selector

            if (selector != null && selector.isNotEmpty()) {
                // Convert selector map to label selector string
                val labelSelector = selector.entries.joinToString(",") { "${it.key}=${it.value}" }

                // Get pods that match the service selector using builder pattern
                val podList = coreApi.listNamespacedPod(namespace)
                    .apply {
                        properties.fieldSelector?.let { fieldSelector(it) }
                        labelSelector(labelSelector)
                    }
                    .execute()

                podList.items?.let { pods.addAll(it) }
            } else {
                logger.warn("Service ${properties.serviceName} does not have any selectors")
            }
        } catch (e: ApiException) {
            logger.error("Failed to get pods from service: ${e.responseBody}", e)
        }

        return pods
    }
}
