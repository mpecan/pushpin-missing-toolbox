package io.github.mpecan.pmt.discovery.aws.instances

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.clients.AutoScalingClientProvider
import io.github.mpecan.pmt.discovery.aws.clients.Ec2ClientProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.autoscaling.AutoScalingClient
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceState
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.Reservation
import software.amazon.awssdk.services.ec2.model.Tag
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import software.amazon.awssdk.services.autoscaling.model.Instance as AsgInstance

@ExtendWith(MockitoExtension::class)
class DefaultEc2InstancesProviderTest {

    private val ec2ClientProvider: Ec2ClientProvider = mock()
    private val autoScalingClientProvider: AutoScalingClientProvider = mock()
    private val ec2Client: Ec2Client = mock()
    private val autoScalingClient: AutoScalingClient = mock()

    private lateinit var provider: DefaultEc2InstancesProvider
    private lateinit var properties: AwsDiscoveryProperties

    @BeforeEach
    fun setUp() {
        provider = DefaultEc2InstancesProvider(ec2ClientProvider, autoScalingClientProvider)
        properties = AwsDiscoveryProperties(
            enabled = true,
            region = "us-east-1",
            tags = mapOf("service" to "pushpin"),
            useAutoScalingGroups = false,
        )

        // Common setup that applies to all tests
        whenever(ec2ClientProvider.getClient(any())).thenReturn(ec2Client)
        whenever(autoScalingClientProvider.getClient(any())).thenReturn(autoScalingClient)
    }

    @Test
    fun `should discover instances based on tags`() {
        // Create mock EC2 instances
        val instance1 = Instance.builder()
            .instanceId("i-12345")
            .privateIpAddress("10.0.0.1")
            .publicIpAddress("54.0.0.1")
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .tags(Tag.builder().key("service").value("pushpin").build())
            .build()

        val instance2 = Instance.builder()
            .instanceId("i-67890")
            .privateIpAddress("10.0.0.2")
            .publicIpAddress("54.0.0.2")
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .tags(Tag.builder().key("service").value("pushpin").build())
            .build()

        // Create mock reservation and response
        val reservation = Reservation.builder()
            .instances(instance1, instance2)
            .build()

        val response = DescribeInstancesResponse.builder()
            .reservations(reservation)
            .build()

        // Mock EC2 client response
        whenever(ec2Client.describeInstances(any<DescribeInstancesRequest>())).thenReturn(response)

        // Call getInstances method
        val instances = provider.getInstances(properties)

        // Verify that the EC2 client was called with appropriate request
        verify(ec2Client).describeInstances(any<DescribeInstancesRequest>())

        // Verify that the instances were returned
        assertEquals(2, instances.size)
        assertTrue(instances.contains(instance1))
        assertTrue(instances.contains(instance2))
    }

    @Test
    fun `should discover instances from Auto Scaling Groups`() {
        // Set properties to use Auto Scaling Groups instead of tags
        val asgProperties = properties.copy(
            useAutoScalingGroups = true,
            tags = emptyMap(), // No tag filtering
        )

        // Create mock ASG instances with InService lifecycle state
        val asgInstance1 = AsgInstance.builder()
            .instanceId("i-asg-12345")
            .lifecycleState("InService")
            .build()

        val asgInstance2 = AsgInstance.builder()
            .instanceId("i-asg-67890")
            .lifecycleState("InService")
            .build()

        // Create Auto Scaling Group
        val asg = AutoScalingGroup.builder()
            .autoScalingGroupName("pushpin-asg")
            .instances(asgInstance1, asgInstance2)
            .build()

        // Create ASG response
        val asgResponse = DescribeAutoScalingGroupsResponse.builder()
            .autoScalingGroups(asg)
            .build()

        // Mock Auto Scaling client response
        whenever(autoScalingClient.describeAutoScalingGroups(any<DescribeAutoScalingGroupsRequest>()))
            .thenReturn(asgResponse)

        // Create mock EC2 instances that match the ASG instance IDs
        val ec2Instance1 = Instance.builder()
            .instanceId("i-asg-12345")
            .privateIpAddress("10.0.1.1")
            .publicIpAddress("54.1.1.1")
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .build()

        val ec2Instance2 = Instance.builder()
            .instanceId("i-asg-67890")
            .privateIpAddress("10.0.1.2")
            .publicIpAddress("54.1.1.2")
            .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
            .build()

        // Create EC2 response for the instance ID lookup
        val reservation = Reservation.builder()
            .instances(ec2Instance1, ec2Instance2)
            .build()

        val ec2Response = DescribeInstancesResponse.builder()
            .reservations(reservation)
            .build()

        // Since we can't easily use argThat for the AWS SDK directly,
        // we'll mock any DescribeInstancesRequest to return our EC2 response
        // This is a simplification for the test, but in real code
        // the implementation would match instance IDs correctly
        whenever(ec2Client.describeInstances(any<DescribeInstancesRequest>())).thenReturn(ec2Response)

        // Call the method under test
        val instances = provider.getInstances(asgProperties)

        // Verify calls to AWS clients
        verify(autoScalingClient).describeAutoScalingGroups(any<DescribeAutoScalingGroupsRequest>())
        verify(ec2Client, atLeastOnce()).describeInstances(any<DescribeInstancesRequest>())

        // Now that we've fixed the implementation to use ASG discovery exclusively,
        // let's verify that the instances were returned
        assertEquals(2, instances.size, "Should have found 2 instances from ASG")

        // Get the discovered instance IDs
        val instanceIds = instances.map { it.instanceId() }

        // Verify both instances were found
        assertTrue(instanceIds.contains("i-asg-12345"), "Should contain instance i-asg-12345")
        assertTrue(instanceIds.contains("i-asg-67890"), "Should contain instance i-asg-67890")
    }

    @Test
    fun `should handle empty responses`() {
        // Mock empty response
        val emptyResponse = DescribeInstancesResponse.builder()
            .reservations(emptyList())
            .build()

        whenever(ec2Client.describeInstances(any<DescribeInstancesRequest>())).thenReturn(emptyResponse)

        // Call getInstances method
        val instances = provider.getInstances(properties)

        // Verify that the EC2 client was called
        verify(ec2Client).describeInstances(any<DescribeInstancesRequest>())

        // Verify that no instances were returned
        assertTrue(instances.isEmpty())
    }
}
