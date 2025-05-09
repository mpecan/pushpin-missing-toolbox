package io.github.mpecan.pmt.discovery.aws.config

import io.github.mpecan.pmt.discovery.aws.AwsDiscovery
import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.converter.DefaultInstanceConverter
import io.github.mpecan.pmt.discovery.aws.converter.InstanceConverter
import io.github.mpecan.pmt.discovery.aws.health.DefaultInstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.health.InstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.instances.DefaultEc2InstancesProvider
import io.github.mpecan.pmt.discovery.aws.instances.Ec2InstancesProvider
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.Instance
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AwsDiscoveryAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AwsDiscoveryAutoConfiguration::class.java))

    @Test
    fun `configuration is not loaded when Ec2Client is not on classpath`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(Ec2Client::class.java))
            .withPropertyValues("pushpin.discovery.aws.enabled=true")
            .run { context ->
                assertNull(context.getBeansOfType(AwsDiscovery::class.java).values.firstOrNull())
            }
    }

    @Test
    fun `configuration is not loaded when aws discovery is disabled`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.aws.enabled=false")
            .run { context ->
                assertNull(context.getBeansOfType(AwsDiscovery::class.java).values.firstOrNull())
            }
    }

    @Test
    fun `configuration is loaded with default beans when aws discovery is enabled`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.aws.enabled=true")
            .run { context ->
                val awsDiscovery = context.getBean(AwsDiscovery::class.java)
                assertNotNull(awsDiscovery)

                // Verify default beans are created
                assertNotNull(context.getBean(Ec2InstancesProvider::class.java))
                assertTrue(context.getBean(Ec2InstancesProvider::class.java) is DefaultEc2InstancesProvider)

                assertNotNull(context.getBean(InstanceHealthChecker::class.java))
                assertTrue(context.getBean(InstanceHealthChecker::class.java) is DefaultInstanceHealthChecker)

                assertNotNull(context.getBean(InstanceConverter::class.java))
                assertTrue(context.getBean(InstanceConverter::class.java) is DefaultInstanceConverter)

                // Verify properties are properly configured
                val properties = context.getBean(AwsDiscoveryProperties::class.java)
                assertNotNull(properties)
                assertTrue(properties.enabled)
            }
    }

    @Test
    fun `configuration respects custom beans`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.aws.enabled=true")
            .withUserConfiguration(CustomBeansConfig::class.java)
            .run { context ->
                val awsDiscovery = context.getBean(AwsDiscovery::class.java)
                assertNotNull(awsDiscovery)

                // Verify custom beans are used
                assertNotNull(context.getBean(Ec2InstancesProvider::class.java))
                assertTrue(context.getBean(Ec2InstancesProvider::class.java) is CustomEc2InstancesProvider)

                assertNotNull(context.getBean(InstanceHealthChecker::class.java))
                assertTrue(context.getBean(InstanceHealthChecker::class.java) is CustomInstanceHealthChecker)

                assertNotNull(context.getBean(InstanceConverter::class.java))
                assertTrue(context.getBean(InstanceConverter::class.java) is CustomInstanceConverter)
            }
    }

    @Configuration
    class CustomBeansConfig {

        @Bean
        fun ec2InstancesProvider(): Ec2InstancesProvider {
            return CustomEc2InstancesProvider()
        }

        @Bean
        fun instanceHealthChecker(): InstanceHealthChecker {
            return CustomInstanceHealthChecker()
        }

        @Bean
        fun instanceConverter(): InstanceConverter {
            return CustomInstanceConverter()
        }
    }

    class CustomEc2InstancesProvider : Ec2InstancesProvider {
        override fun getInstances(properties: AwsDiscoveryProperties) = emptyList<Instance>()
    }

    class CustomInstanceHealthChecker : InstanceHealthChecker {
        override fun isHealthy(
            instance: Instance,
            properties: AwsDiscoveryProperties
        ) = true
    }

    class CustomInstanceConverter : InstanceConverter {
        override fun toPushpinServer(
            instance: Instance,
            properties: AwsDiscoveryProperties
        ) =
            mock(io.github.mpecan.pmt.model.PushpinServer::class.java)
    }
}