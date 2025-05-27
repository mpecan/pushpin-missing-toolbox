package io.github.mpecan.pmt.discovery.aws.converter

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.model.PushpinServer
import software.amazon.awssdk.services.ec2.model.Instance

/**
 * Interface for converting EC2 instances to PushpinServer objects.
 */
interface InstanceConverter {
    /**
     * Converts an EC2 instance to a PushpinServer.
     */
    fun toPushpinServer(instance: Instance, properties: AwsDiscoveryProperties): PushpinServer
}
