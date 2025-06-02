package io.github.mpecan.pmt.discovery.aws.health

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.clients.Ec2ClientProvider
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.SummaryStatus

/**
 * Default implementation of InstanceHealthChecker that checks if an EC2 instance is running and healthy.
 */
open class DefaultInstanceHealthChecker(
    private val ec2ClientProvider: Ec2ClientProvider = Ec2ClientProvider(),
) : InstanceHealthChecker {
    private val logger = LoggerFactory.getLogger(DefaultInstanceHealthChecker::class.java)

    override fun isHealthy(
        instance: Instance,
        properties: AwsDiscoveryProperties,
    ): Boolean {
        // Check if instance is in running state
        val isRunning = instance.state().name() == InstanceStateName.RUNNING

        // If health checks are disabled, just return the running state
        if (!properties.instanceHealthCheckEnabled) {
            return isRunning
        }

        // Check instance status if health checks are enabled
        if (isRunning) {
            try {
                val ec2Client = ec2ClientProvider.getClient(properties)
                val statusResponse =
                    ec2Client.describeInstanceStatus(
                        DescribeInstanceStatusRequest
                            .builder()
                            .instanceIds(instance.instanceId())
                            .includeAllInstances(true)
                            .build(),
                    )

                val instanceStatus = statusResponse.instanceStatuses().firstOrNull()
                if (instanceStatus != null) {
                    // Check both system and instance status
                    val systemStatus = instanceStatus.systemStatus().status() == SummaryStatus.OK
                    val instanceHealthStatus = instanceStatus.instanceStatus().status() == SummaryStatus.OK

                    return systemStatus && instanceHealthStatus
                }
            } catch (e: Exception) {
                logger.warn("Failed to check instance health status for ${instance.instanceId()}: ${e.message}")
                // If we can't check health, assume it's healthy if it's running
                return true
            }
        }

        return isRunning
    }
}
