package io.github.mpecan.pmt.discovery.aws

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for AWS-based discovery.
 *
 * @property enabled Whether AWS-based discovery is enabled
 * @property region The AWS region to use
 * @property endpoint Custom endpoint URL for AWS services (useful for testing with localstack)
 * @property tags Map of tags to use for filtering EC2 instances (key-value pairs)
 * @property useAutoScalingGroups Whether to discover instances from Auto Scaling Groups
 * @property autoScalingGroupNames List of Auto Scaling Group names to filter instances by
 * @property assumeRoleArn ARN of IAM role to assume for discovery (optional)
 * @property refreshCacheMinutes How often to refresh the EC2 instance cache (in minutes)
 * @property instanceHealthCheckEnabled Whether to check EC2 instance health status
 * @property port Default HTTP port for Pushpin servers
 * @property controlPort Default control port for Pushpin servers
 * @property publishPort Default publish port for Pushpin servers
 * @property healthCheckPath Default health check path for Pushpin servers
 * @property privateIp Whether to use private IP addresses (true) or public (false)
 */
@ConfigurationProperties(prefix = "pushpin.discovery.aws")
data class AwsDiscoveryProperties(
    val enabled: Boolean = false,
    val region: String = "us-east-1",
    val endpoint: String? = null,
    val tags: Map<String, String> = mapOf("service" to "pushpin"),
    val useAutoScalingGroups: Boolean = false,
    val autoScalingGroupNames: List<String> = emptyList(),
    val assumeRoleArn: String? = null,
    val refreshCacheMinutes: Int = 5,
    val instanceHealthCheckEnabled: Boolean = true,
    val port: Int = 7999,
    val controlPort: Int = 5564,
    val publishPort: Int = 5560,
    val healthCheckPath: String = "/api/health/check",
    val privateIp: Boolean = true,
)
