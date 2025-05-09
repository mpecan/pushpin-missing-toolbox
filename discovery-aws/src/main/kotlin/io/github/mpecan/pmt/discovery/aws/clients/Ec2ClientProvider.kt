package io.github.mpecan.pmt.discovery.aws.clients

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder
import java.net.URI
import java.util.function.Supplier

/**
 * Provider for EC2 clients.
 */
class Ec2ClientProvider(
    private val credentialsProvider: AwsCredentialsProvider = AwsCredentialsProvider(),
    private val clientBuilderSupplier: Supplier<Ec2ClientBuilder> = Supplier { Ec2Client.builder() }
) {
    /**
     * Gets or creates an EC2 client for the given properties.
     */
    fun getClient(properties: AwsDiscoveryProperties): Ec2Client {
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

