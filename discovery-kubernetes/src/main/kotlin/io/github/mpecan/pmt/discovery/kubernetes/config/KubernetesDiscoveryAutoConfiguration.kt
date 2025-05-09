package io.github.mpecan.pmt.discovery.kubernetes.config

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscovery
import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.github.mpecan.pmt.discovery.kubernetes.clients.KubernetesClientProvider
import io.github.mpecan.pmt.discovery.kubernetes.converter.DefaultPodConverter
import io.github.mpecan.pmt.discovery.kubernetes.converter.PodConverter
import io.github.mpecan.pmt.discovery.kubernetes.health.DefaultPodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.health.PodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.pods.DefaultKubernetesPodProvider
import io.github.mpecan.pmt.discovery.kubernetes.pods.KubernetesPodProvider
import io.kubernetes.client.openapi.ApiClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Autoconfiguration for Kubernetes-based Pushpin discovery.
 * 
 * This autoconfiguration is conditionally enabled:
 * - When the Kubernetes ApiClient class is available on the classpath
 * - When the pushpin.discovery.kubernetes.enabled property is true (defaults to false)
 */
@AutoConfiguration
@ConditionalOnClass(ApiClient::class)
@EnableConfigurationProperties(KubernetesDiscoveryProperties::class)
@ConditionalOnProperty(
    prefix = "pushpin.discovery.kubernetes",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class KubernetesDiscoveryAutoConfiguration {

    /**
     * Creates a KubernetesClientProvider bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kubernetesClientProvider(): KubernetesClientProvider {
        return KubernetesClientProvider()
    }
    
    /**
     * Creates a DefaultKubernetesPodProvider bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kubernetesPodProvider(clientProvider: KubernetesClientProvider): KubernetesPodProvider {
        return DefaultKubernetesPodProvider(clientProvider)
    }
    
    /**
     * Creates a DefaultPodHealthChecker bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun podHealthChecker(): PodHealthChecker {
        return DefaultPodHealthChecker()
    }
    
    /**
     * Creates a DefaultPodConverter bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun podConverter(): PodConverter {
        return DefaultPodConverter()
    }
    
    /**
     * Creates a KubernetesDiscovery bean.
     */
    @Bean
    @ConditionalOnMissingBean
    fun kubernetesDiscovery(
        properties: KubernetesDiscoveryProperties,
        podProvider: KubernetesPodProvider,
        podHealthChecker: PodHealthChecker,
        podConverter: PodConverter
    ): KubernetesDiscovery {
        return KubernetesDiscovery(
            properties = properties,
            podProvider = podProvider,
            podHealthChecker = podHealthChecker,
            podConverter = podConverter
        )
    }
}