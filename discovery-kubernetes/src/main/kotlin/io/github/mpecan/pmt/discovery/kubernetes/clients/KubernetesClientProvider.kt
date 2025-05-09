package io.github.mpecan.pmt.discovery.kubernetes.clients

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Provider for Kubernetes client API instances.
 * 
 * This class manages the creation of Kubernetes client APIs with proper configuration
 * based on the provided properties.
 */
class KubernetesClientProvider {
    private val logger = LoggerFactory.getLogger(KubernetesClientProvider::class.java)
    
    /**
     * Get a properly configured CoreV1Api client.
     * 
     * @param properties The Kubernetes discovery properties.
     * @return A configured CoreV1Api client.
     */
    fun getCoreApi(properties: KubernetesDiscoveryProperties): CoreV1Api {
        val apiClient = createApiClient(properties)
        return CoreV1Api(apiClient)
    }
    
    /**
     * Creates an ApiClient based on the provided properties.
     * 
     * This will either use:
     * - The kubeconfig file at the specified path
     * - The default kubeconfig location (~/.kube/config)
     * - In-cluster configuration if running inside Kubernetes
     * 
     * @param properties The Kubernetes discovery properties.
     * @return A configured ApiClient.
     */
    private fun createApiClient(properties: KubernetesDiscoveryProperties): ApiClient {
        logger.debug("Creating Kubernetes ApiClient")
        
        val apiClient = when {
            // If a specific kubeconfig path is provided
            properties.kubeConfigPath != null && Files.exists(Paths.get(properties.kubeConfigPath)) -> {
                logger.debug("Using kubeconfig from: ${properties.kubeConfigPath}")
                val inputStream = FileInputStream(properties.kubeConfigPath)
                Config.fromConfig(inputStream)
            }
            
            // If we can find the default kubeconfig
            Files.exists(Paths.get(System.getProperty("user.home"), ".kube", "config")) -> {
                logger.debug("Using default kubeconfig from ~/.kube/config")
                Config.defaultClient()
            }
            
            // Otherwise, assume we're running in a cluster
            else -> {
                logger.debug("Using in-cluster configuration")
                try {
                    Config.fromCluster()
                } catch (e: Exception) {
                    logger.warn("Failed to load in-cluster config, falling back to default: ${e.message}")
                    Config.defaultClient()
                }
            }
        }
        
        // Configure timeouts
        apiClient.httpClient = apiClient.httpClient.newBuilder()
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        return apiClient
    }
}