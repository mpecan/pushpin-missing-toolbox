package io.github.mpecan.pmt.discovery.aws.clients

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.autoscaling.AutoScalingClient
import software.amazon.awssdk.services.autoscaling.AutoScalingClientBuilder
import java.net.URI
import java.util.function.Supplier

/**
 * Provider for Auto Scaling clients.
 */
class AutoScalingClientProvider(
    private val credentialsProvider: AwsCredentialsProvider = AwsCredentialsProvider(),
    private val clientBuilderSupplier: Supplier<AutoScalingClientBuilder> = Supplier { AutoScalingClient.builder() },
) {
    /**
     * Gets or creates an Auto Scaling client for the given properties.
     */
    fun getClient(properties: AwsDiscoveryProperties): AutoScalingClient {
        val region = Region.of(properties.region)
        val credentials = credentialsProvider.getCredentials(properties)

        val builder = clientBuilderSupplier.get()
            .region(region)
            .credentialsProvider(credentials)

        // Add custom endpoint if configured (useful for testing with localstack)
        if (!properties.endpoint.isNullOrBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint))
        }

        return builder.build()
    }
}
