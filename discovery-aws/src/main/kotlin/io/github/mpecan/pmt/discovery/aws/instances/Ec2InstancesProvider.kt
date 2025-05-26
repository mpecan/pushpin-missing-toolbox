package io.github.mpecan.pmt.discovery.aws.instances

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import software.amazon.awssdk.services.ec2.model.Instance

/**
 * Interface for providing EC2 instances.
 */
interface Ec2InstancesProvider {
    /**
     * Returns a list of EC2 instances based on the provided properties.
     */
    fun getInstances(properties: AwsDiscoveryProperties): List<Instance>
}
