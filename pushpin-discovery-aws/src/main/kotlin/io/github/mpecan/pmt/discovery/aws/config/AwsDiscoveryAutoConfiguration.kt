package io.github.mpecan.pmt.discovery.aws.config

import io.github.mpecan.pmt.discovery.aws.AwsDiscovery
import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.converter.DefaultInstanceConverter
import io.github.mpecan.pmt.discovery.aws.converter.InstanceConverter
import io.github.mpecan.pmt.discovery.aws.health.DefaultInstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.health.InstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.instances.DefaultEc2InstancesProvider
import io.github.mpecan.pmt.discovery.aws.instances.Ec2InstancesProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.services.ec2.Ec2Client

/**
 * Autoconfiguration for AWS-based Pushpin discovery.
 * * This autoconfiguration is conditionally enabled:
 * - When the EC2Client class is available on the classpath
 * - When the pushpin.discovery.aws.enabled property is true (defaults to false)
 */
@AutoConfiguration
@ConditionalOnClass(Ec2Client::class)
@EnableConfigurationProperties(AwsDiscoveryProperties::class)
@ConditionalOnProperty(
    prefix = "pushpin.discovery.aws",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class AwsDiscoveryAutoConfiguration {
    /**
     * Creates a DefaultEc2InstancesProvider bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun ec2InstancesProvider(): Ec2InstancesProvider = DefaultEc2InstancesProvider()

    /**
     * Creates a DefaultInstanceHealthChecker bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun instanceHealthChecker(): InstanceHealthChecker = DefaultInstanceHealthChecker()

    /**
     * Creates a DefaultInstanceConverter bean if none exists.
     */
    @Bean
    @ConditionalOnMissingBean
    fun instanceConverter(): InstanceConverter = DefaultInstanceConverter()

    /**
     * Creates an AwsDiscovery bean.
     */
    @Bean
    @ConditionalOnMissingBean
    fun awsDiscovery(
        properties: AwsDiscoveryProperties,
        instancesProvider: Ec2InstancesProvider,
        instanceHealthChecker: InstanceHealthChecker,
        instanceConverter: InstanceConverter,
    ): AwsDiscovery =
        AwsDiscovery(
            properties = properties,
            instancesProvider = instancesProvider,
            instanceHealthChecker = instanceHealthChecker,
            instanceConverter = instanceConverter,
        )
}
