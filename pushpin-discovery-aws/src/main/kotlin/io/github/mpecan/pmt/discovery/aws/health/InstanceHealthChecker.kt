package io.github.mpecan.pmt.discovery.aws.health

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import software.amazon.awssdk.services.ec2.model.Instance

/**
 * Interface for checking if an EC2 instance is healthy.
 */
fun interface InstanceHealthChecker {
    /**
     * Checks if an EC2 instance is healthy.
     */
    fun isHealthy(
        instance: Instance,
        properties: AwsDiscoveryProperties,
    ): Boolean
}
