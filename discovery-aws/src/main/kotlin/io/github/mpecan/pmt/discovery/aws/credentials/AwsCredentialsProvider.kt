package io.github.mpecan.pmt.discovery.aws.credentials

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import java.util.UUID

/**
 * Provider for AWS credentials.
 */
class AwsCredentialsProvider {
    /**
     * Gets or creates AWS credentials provider for the given properties.
     */
    fun getCredentials(properties: AwsDiscoveryProperties): software.amazon.awssdk.auth.credentials.AwsCredentialsProvider {
        val defaultCredentialsProvider = DefaultCredentialsProvider.create()
        
        // If a role ARN is specified, create credentials provider that assumes that role
        if (!properties.assumeRoleArn.isNullOrBlank()) {
            val roleSessionName = "pushpin-discovery-${UUID.randomUUID()}"
            
            val stsClient = StsClient.builder()
                .region(Region.of(properties.region))
                .credentialsProvider(defaultCredentialsProvider)
                .build()
            
            val assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(properties.assumeRoleArn)
                .roleSessionName(roleSessionName)
                .build()
            
            return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(assumeRoleRequest)
                .build()
        }
        
        return defaultCredentialsProvider
    }
}