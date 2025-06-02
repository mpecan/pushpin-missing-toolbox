package io.github.mpecan.pmt.discovery.aws.health

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.clients.Ec2ClientProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceState
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.InstanceStatus
import software.amazon.awssdk.services.ec2.model.InstanceStatusSummary
import software.amazon.awssdk.services.ec2.model.SummaryStatus
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DefaultInstanceHealthCheckerTest {
    @Mock
    private lateinit var ec2ClientProvider: Ec2ClientProvider

    @Mock
    private lateinit var ec2Client: Ec2Client

    private lateinit var healthChecker: DefaultInstanceHealthChecker
    private lateinit var properties: AwsDiscoveryProperties

    // Test instances
    private val runningInstance =
        Instance
            .builder()
            .instanceId("i-running")
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .build()

    private val stoppedInstance =
        Instance
            .builder()
            .instanceId("i-stopped")
            .state(InstanceState.builder().name(InstanceStateName.STOPPED).build())
            .build()

    @BeforeEach
    fun setUp() {
        healthChecker = DefaultInstanceHealthChecker(ec2ClientProvider)
        properties =
            AwsDiscoveryProperties(
                enabled = true,
                instanceHealthCheckEnabled = true,
            )
    }

    @Test
    fun `should return false for stopped instances`() {
        // Verify that stopped instances are considered unhealthy
        assertFalse(healthChecker.isHealthy(stoppedInstance, properties))
    }

    @Test
    fun `should return true for running instances when health check is disabled`() {
        // Set up properties with health checks disabled
        val propertiesWithoutHealthCheck = properties.copy(instanceHealthCheckEnabled = false)

        // Verify that running instances are considered healthy when health checks are disabled
        assertTrue(healthChecker.isHealthy(runningInstance, propertiesWithoutHealthCheck))
    }

    @Test
    fun `should check EC2 status when health check is enabled`() {
        // Setup EC2 client provider
        `when`(ec2ClientProvider.getClient(properties)).thenReturn(ec2Client)

        // Create mock status response with healthy status
        val healthyStatus =
            InstanceStatus
                .builder()
                .instanceId("i-running")
                .instanceStatus(InstanceStatusSummary.builder().status(SummaryStatus.OK).build())
                .systemStatus(InstanceStatusSummary.builder().status(SummaryStatus.OK).build())
                .build()

        val statusResponse =
            DescribeInstanceStatusResponse
                .builder()
                .instanceStatuses(healthyStatus)
                .build()

        // Mock EC2 client response
        `when`(ec2Client.describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())).thenReturn(
            statusResponse,
        )

        // Verify that the instance is considered healthy
        assertTrue(healthChecker.isHealthy(runningInstance, properties))

        // Verify that the EC2 client was called to check status
        verify(ec2Client).describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())
    }

    @Test
    fun `should return false for unhealthy instances`() {
        // Setup EC2 client provider
        `when`(ec2ClientProvider.getClient(properties)).thenReturn(ec2Client)

        // Create mock status response with impaired status
        val impairedStatus =
            InstanceStatus
                .builder()
                .instanceId("i-running")
                .instanceStatus(InstanceStatusSummary.builder().status(SummaryStatus.IMPAIRED).build())
                .systemStatus(InstanceStatusSummary.builder().status(SummaryStatus.OK).build())
                .build()

        val statusResponse =
            DescribeInstanceStatusResponse
                .builder()
                .instanceStatuses(impairedStatus)
                .build()

        // Mock EC2 client response
        `when`(ec2Client.describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())).thenReturn(
            statusResponse,
        )

        // Verify that the instance is considered unhealthy
        assertFalse(healthChecker.isHealthy(runningInstance, properties))

        // Verify that the EC2 client was called to check status
        verify(ec2Client).describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())
    }

    @Test
    fun `should handle empty status response`() {
        // Setup EC2 client provider
        `when`(ec2ClientProvider.getClient(properties)).thenReturn(ec2Client)

        // Create empty status response
        val emptyStatusResponse =
            DescribeInstanceStatusResponse
                .builder()
                .instanceStatuses(emptyList())
                .build()

        // Mock EC2 client response
        `when`(ec2Client.describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())).thenReturn(
            emptyStatusResponse,
        )

        // Verify that instances with no status information are considered running
        // This is the default behavior when no status is available
        assertTrue(healthChecker.isHealthy(runningInstance, properties))

        // Verify that the EC2 client was called to check status
        verify(ec2Client).describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())
    }

    @Test
    fun `should handle exceptions when checking status`() {
        // Setup EC2 client provider
        `when`(ec2ClientProvider.getClient(properties)).thenReturn(ec2Client)

        // Mock EC2 client to throw an exception
        `when`(ec2Client.describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>()))
            .thenThrow(RuntimeException("Test exception"))

        // Verify that instances are considered healthy when an exception occurs
        // This is the default behavior to avoid false negatives
        assertTrue(healthChecker.isHealthy(runningInstance, properties))

        // Verify that the EC2 client was called to check status
        verify(ec2Client).describeInstanceStatus(org.mockito.kotlin.any<DescribeInstanceStatusRequest>())
    }
}
