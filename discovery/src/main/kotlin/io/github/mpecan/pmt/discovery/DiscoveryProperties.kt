package io.github.mpecan.pmt.discovery

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for the Pushpin discovery system.
 *
 * @property enabled Whether the discovery system is enabled
 * @property refreshInterval The interval at which to refresh the server list
 * @property configuration Configuration for the configuration-based discovery
 * @property aws Configuration for AWS-based discovery
 * @property kubernetes Configuration for Kubernetes-based discovery
 */
@ConfigurationProperties(prefix = "pushpin.discovery")
data class DiscoveryProperties(
    val enabled: Boolean = true,
    val refreshInterval: Duration = Duration.ofMinutes(1),
    val configuration: ConfigurationDiscoveryProperties = ConfigurationDiscoveryProperties(),
    val aws: AwsDiscoveryProperties = AwsDiscoveryProperties(),
    val kubernetes: KubernetesDiscoveryProperties = KubernetesDiscoveryProperties()
)

/**
 * Configuration properties for configuration-based discovery.
 *
 * @property enabled Whether configuration-based discovery is enabled
 */
data class ConfigurationDiscoveryProperties(
    val enabled: Boolean = true
)

/**
 * Configuration properties for AWS-based discovery.
 *
 * @property enabled Whether AWS-based discovery is enabled
 * @property region The AWS region to use
 * @property tagKey The tag key to use for filtering instances
 * @property tagValue The tag value to use for filtering instances
 */
data class AwsDiscoveryProperties(
    val enabled: Boolean = false,
    val region: String = "us-east-1",
    val tagKey: String = "service",
    val tagValue: String = "pushpin"
)

/**
 * Configuration properties for Kubernetes-based discovery.
 *
 * @property enabled Whether Kubernetes-based discovery is enabled
 * @property namespace The Kubernetes namespace to use
 * @property labelSelector The label selector to use for filtering pods
 */
data class KubernetesDiscoveryProperties(
    val enabled: Boolean = false,
    val namespace: String = "default",
    val labelSelector: String = "app=pushpin"
)