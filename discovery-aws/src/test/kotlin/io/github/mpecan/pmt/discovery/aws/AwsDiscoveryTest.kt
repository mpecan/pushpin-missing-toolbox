package io.github.mpecan.pmt.discovery.aws

import io.github.mpecan.pmt.discovery.aws.converter.InstanceConverter
import io.github.mpecan.pmt.discovery.aws.health.InstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.instances.Ec2InstancesProvider
import io.github.mpecan.pmt.model.PushpinServer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import reactor.test.StepVerifier
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceState
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.Tag

/**
 * Tests for AwsDiscovery class.
 */
@ExtendWith(MockitoExtension::class)
class AwsDiscoveryTest {

    @Mock
    private lateinit var mockInstancesProvider: Ec2InstancesProvider
    
    @Mock
    private lateinit var mockHealthChecker: InstanceHealthChecker
    
    @Mock
    private lateinit var mockConverter: InstanceConverter

    private lateinit var properties: AwsDiscoveryProperties
    private lateinit var awsDiscovery: AwsDiscovery

    private val testInstance1 = Instance.builder()
        .instanceId("i-test1")
        .privateIpAddress("10.0.0.1")
        .publicIpAddress("54.0.0.1")
        .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
        .tags(Tag.builder().key("Name").value("pushpin-1").build())
        .build()
        
    private val testInstance2 = Instance.builder()
        .instanceId("i-test2")
        .privateIpAddress("10.0.0.2")
        .publicIpAddress("54.0.0.2")
        .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
        .tags(Tag.builder().key("Name").value("pushpin-2").build())
        .build()
        
    private val testServer1 = PushpinServer(
        id = "pushpin-1",
        host = "10.0.0.1",
        port = 7999,
        controlPort = 5564,
        publishPort = 5560
    )
    
    private val testServer2 = PushpinServer(
        id = "pushpin-2",
        host = "10.0.0.2",
        port = 7999,
        controlPort = 5564,
        publishPort = 5560
    )

    @BeforeEach
    fun setUp() {
        // Create default properties for testing
        properties = AwsDiscoveryProperties(
            enabled = true,
            region = "us-east-1",
            endpoint = "http://localhost:4566",
            tags = mapOf("service" to "pushpin"),
            useAutoScalingGroups = false,
            refreshCacheMinutes = 5,
            instanceHealthCheckEnabled = true,
            port = 7999,
            controlPort = 5564,
            publishPort = 5560,
            privateIp = true
        )

        // Create AWS discovery with mocked components
        awsDiscovery = AwsDiscovery(
            properties = properties,
            instancesProvider = mockInstancesProvider,
            instanceHealthChecker = mockHealthChecker,
            instanceConverter = mockConverter
        )
    }

    @Test
    fun `isEnabled should reflect properties setting`() {
        // When enabled is true
        properties = properties.copy(enabled = true)
        awsDiscovery = AwsDiscovery(
            properties = properties,
            instancesProvider = mockInstancesProvider,
            instanceHealthChecker = mockHealthChecker,
            instanceConverter = mockConverter
        )
        assertTrue(awsDiscovery.isEnabled())

        // When enabled is false
        properties = properties.copy(enabled = false)
        awsDiscovery = AwsDiscovery(
            properties = properties,
            instancesProvider = mockInstancesProvider,
            instanceHealthChecker = mockHealthChecker,
            instanceConverter = mockConverter
        )
        assertFalse(awsDiscovery.isEnabled())
    }

    @Test
    fun `discoverServers should return empty flux when disabled`() {
        // Set the discovery to disabled
        properties = properties.copy(enabled = false)
        awsDiscovery = AwsDiscovery(
            properties = properties,
            instancesProvider = mockInstancesProvider,
            instanceHealthChecker = mockHealthChecker,
            instanceConverter = mockConverter
        )

        // Verify that the result is an empty flux
        StepVerifier.create(awsDiscovery.discoverServers())
            .expectComplete()
            .verify()
    }

    @Test
    fun `discoverServers should return servers from EC2 instances`() {
        // Setup mock instances provider to return test instances
        `when`(mockInstancesProvider.getInstances(properties)).thenReturn(listOf(testInstance1, testInstance2))
        
        // Setup mock health checker to return true for all instances
        `when`(mockHealthChecker.isHealthy(testInstance1, properties)).thenReturn(true)
        `when`(mockHealthChecker.isHealthy(testInstance2, properties)).thenReturn(true)
        
        // Setup mock converter to return test servers
        `when`(mockConverter.toPushpinServer(testInstance1, properties)).thenReturn(testServer1)
        `when`(mockConverter.toPushpinServer(testInstance2, properties)).thenReturn(testServer2)
        
        // Call discoverServers and verify the result
        StepVerifier.create(awsDiscovery.discoverServers())
            .expectNext(testServer1)
            .expectNext(testServer2)
            .expectComplete()
            .verify()
    }
    
    @Test
    fun `discoverServers should filter unhealthy instances`() {
        // Setup mock instances provider to return test instances
        `when`(mockInstancesProvider.getInstances(properties)).thenReturn(listOf(testInstance1, testInstance2))
        
        // Setup mock health checker to return true for first instance and false for second
        `when`(mockHealthChecker.isHealthy(testInstance1, properties)).thenReturn(true)
        `when`(mockHealthChecker.isHealthy(testInstance2, properties)).thenReturn(false)
        
        // Setup mock converter to return test servers
        `when`(mockConverter.toPushpinServer(testInstance1, properties)).thenReturn(testServer1)
        
        // Call discoverServers and verify only the healthy instance is returned
        StepVerifier.create(awsDiscovery.discoverServers())
            .expectNext(testServer1)
            .expectComplete()
            .verify()
    }
}