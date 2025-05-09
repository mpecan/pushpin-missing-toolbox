package io.github.mpecan.pmt.discovery.aws.converter

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.model.PushpinServer
import software.amazon.awssdk.services.ec2.model.Instance

/**
 * Default implementation of InstanceConverter that converts EC2 instances to PushpinServer objects.
 */
class DefaultInstanceConverter : InstanceConverter {
    override fun toPushpinServer(instance: Instance, properties: AwsDiscoveryProperties): PushpinServer {
        // Get appropriate IP address based on configuration
        val host = if (properties.privateIp) {
            instance.privateIpAddress()
        } else {
            instance.publicIpAddress() ?: instance.privateIpAddress()
        }

        // Get server ID from instance ID or Name tag
        val nameTag = instance.tags().find { it.key() == "Name" }
        val serverId = nameTag?.value() ?: instance.instanceId()

        return PushpinServer(
            id = serverId,
            host = host,
            port = properties.port,
            controlPort = properties.controlPort,
            publishPort = properties.publishPort,
            healthCheckPath = properties.healthCheckPath
        )
    }
}