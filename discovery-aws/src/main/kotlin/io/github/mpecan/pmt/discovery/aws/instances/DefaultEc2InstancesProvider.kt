package io.github.mpecan.pmt.discovery.aws.instances

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.clients.AutoScalingClientProvider
import io.github.mpecan.pmt.discovery.aws.clients.Ec2ClientProvider
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.autoscaling.AutoScalingClient
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.Filter
import software.amazon.awssdk.services.ec2.model.Instance

/**
 * Default implementation of Ec2InstancesProvider that discovers instances from EC2 and Auto Scaling Groups.
 */
class DefaultEc2InstancesProvider(
    private val ec2ClientProvider: Ec2ClientProvider = Ec2ClientProvider(),
    private val autoScalingClientProvider: AutoScalingClientProvider = AutoScalingClientProvider()
) : Ec2InstancesProvider {
    private val logger = LoggerFactory.getLogger(DefaultEc2InstancesProvider::class.java)

    override fun getInstances(properties: AwsDiscoveryProperties): List<Instance> {
        val instances = mutableListOf<Instance>()

        try {
            // Get EC2 client
            val ec2Client = ec2ClientProvider.getClient(properties)

            // If ASG discovery is enabled, use only ASG discovery
            // Otherwise, use tag-based discovery
            if (properties.useAutoScalingGroups) {
                // Use ASG discovery
                val autoScalingClient = autoScalingClientProvider.getClient(properties)
                instances.addAll(getInstancesFromAutoScalingGroups(ec2Client, autoScalingClient, properties))
                logger.debug("Discovered ${instances.size} instances from Auto Scaling Groups")
            } else {
                // Use tag-based discovery
                instances.addAll(getInstancesFromTags(ec2Client, properties))
                logger.debug("Discovered ${instances.size} instances from tags")
            }
        } catch (e: Exception) {
            logger.error("Error discovering EC2 instances: ${e.message}", e)
        }

        return instances
    }

    /**
     * Discovers EC2 instances based on tags.
     */
    private fun getInstancesFromTags(ec2Client: Ec2Client, properties: AwsDiscoveryProperties): List<Instance> {
        val instances = mutableListOf<Instance>()

        try {
            // Create filters based on configuration
            val filters = buildTagFilters(properties)

            // Describe EC2 instances with the specified filters
            val request = DescribeInstancesRequest.builder()
                .filters(filters)
                .build()

            var response = ec2Client.describeInstances(request)
            var nextToken = response.nextToken()

            // Process the first response
            response.reservations().forEach { reservation ->
                reservation.instances().forEach { instance ->
                    instances.add(instance)
                }
            }

            // Process additional responses if there's pagination
            while (nextToken != null) {
                response = ec2Client.describeInstances(
                    DescribeInstancesRequest.builder()
                        .filters(filters)
                        .nextToken(nextToken)
                        .build()
                )

                response.reservations().forEach { reservation ->
                    reservation.instances().forEach { instance ->
                        instances.add(instance)
                    }
                }

                nextToken = response.nextToken()
            }

            logger.debug("Discovered ${instances.size} instances from tags")
        } catch (e: Exception) {
            logger.error("Failed to discover instances from tags: ${e.message}", e)
        }

        return instances
    }

    /**
     * Discovers EC2 instances that are part of Auto Scaling Groups.
     */
    private fun getInstancesFromAutoScalingGroups(
        ec2Client: Ec2Client,
        autoScalingClient: AutoScalingClient,
        properties: AwsDiscoveryProperties
    ): List<Instance> {
        val instances = mutableListOf<Instance>()

        try {
            logger.debug("Discovering instances from Auto Scaling Groups")

            // Get all Auto Scaling Groups or filter by names if specified
            val request = if (properties.autoScalingGroupNames.isNotEmpty()) {
                DescribeAutoScalingGroupsRequest.builder()
                    .autoScalingGroupNames(properties.autoScalingGroupNames)
                    .build()
            } else {
                DescribeAutoScalingGroupsRequest.builder().build()
            }

            // Collect all instance IDs from the Auto Scaling Groups
            val instanceIds = mutableSetOf<String>()
            var nextToken: String? = null

            do {
                val response = if (nextToken != null) {
                    autoScalingClient.describeAutoScalingGroups(
                        DescribeAutoScalingGroupsRequest.builder()
                            .nextToken(nextToken)
                            .apply {
                                if (properties.autoScalingGroupNames.isNotEmpty()) {
                                    autoScalingGroupNames(properties.autoScalingGroupNames)
                                }
                            }
                            .build()
                    )
                } else {
                    autoScalingClient.describeAutoScalingGroups(request)
                }

                response.autoScalingGroups().forEach { asg ->
                    asg.instances().forEach { instance ->
                        if (instance.lifecycleState().toString() == "InService") {
                            instanceIds.add(instance.instanceId())
                        }
                    }
                }

                nextToken = response.nextToken()
            } while (nextToken != null)

            // If we found instances in ASGs, get their details from EC2
            if (instanceIds.isNotEmpty()) {
                // Fetch instance details in batches of 100 (AWS API limit)
                instanceIds.chunked(100).forEach { chunk ->
                    val describeRequest = DescribeInstancesRequest.builder()
                        .instanceIds(chunk)
                        .build()

                    val describeResponse = ec2Client.describeInstances(describeRequest)

                    describeResponse.reservations().forEach { reservation ->
                        reservation.instances().forEach { instance ->
                            instances.add(instance)
                        }
                    }
                }

                logger.debug("Found ${instanceIds.size} instances from Auto Scaling Groups")
            }
        } catch (e: Exception) {
            logger.error("Failed to discover instances from Auto Scaling Groups: ${e.message}", e)
        }

        return instances
    }

    /**
     * Builds tag filters for EC2 instance discovery based on configured tags.
     */
    private fun buildTagFilters(properties: AwsDiscoveryProperties): List<Filter> {
        val filters = mutableListOf<Filter>()

        // Add tag filters
        properties.tags.forEach { (key, value) ->
            filters.add(
                Filter.builder()
                    .name("tag:$key")
                    .values(value)
                    .build()
            )
        }

        // Add filter for running instances
        filters.add(
            Filter.builder()
                .name("instance-state-name")
                .values("running")
                .build()
        )

        return filters
    }
}