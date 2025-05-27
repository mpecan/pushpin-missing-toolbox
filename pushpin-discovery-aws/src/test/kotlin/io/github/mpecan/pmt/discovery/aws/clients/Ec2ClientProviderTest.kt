package io.github.mpecan.pmt.discovery.aws.clients

import io.github.mpecan.pmt.discovery.aws.AwsDiscoveryProperties
import io.github.mpecan.pmt.discovery.aws.credentials.AwsCredentialsProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder
import java.net.URI
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider as SdkCredentialsProvider

@ExtendWith(MockitoExtension::class)
class Ec2ClientProviderTest {

    @Test
    fun `should create EC2 client with default settings`() {
        // Arrange
        val mockCredentialsProvider = mock<AwsCredentialsProvider>()
        val mockAwsCredentials = mock<SdkCredentialsProvider>()
        val mockEc2ClientBuilder = mock<Ec2ClientBuilder>()
        val mockEc2Client = mock<Ec2Client>()

        val properties = AwsDiscoveryProperties(
            region = "us-west-2",
        )

        // Setup mocks for builder pattern
        whenever(mockCredentialsProvider.getCredentials(any())).thenReturn(mockAwsCredentials)
        whenever(mockEc2ClientBuilder.region(any<Region>())).thenReturn(mockEc2ClientBuilder)
        whenever(mockEc2ClientBuilder.credentialsProvider(any<SdkCredentialsProvider>())).thenReturn(
            mockEc2ClientBuilder,
        )
        whenever(mockEc2ClientBuilder.build()).thenReturn(mockEc2Client)

        // Create provider with constructor-injected mocked dependencies
        val builderSupplier = Supplier { mockEc2ClientBuilder }
        val provider = Ec2ClientProvider(mockCredentialsProvider, builderSupplier)

        // Act
        val client = provider.getClient(properties)

        // Assert
        assertNotNull(client)
        assertEquals(mockEc2Client, client)

        // Verify correct region and credentials were set
        verify(mockEc2ClientBuilder).region(Region.of("us-west-2"))
        verify(mockEc2ClientBuilder).credentialsProvider(mockAwsCredentials)
        verify(mockEc2ClientBuilder).build()
        verify(mockCredentialsProvider).getCredentials(properties)
    }

    @Test
    fun `should use custom endpoint when provided`() {
        // Arrange
        val mockCredentialsProvider = mock<AwsCredentialsProvider>()
        val mockAwsCredentials = mock<SdkCredentialsProvider>()
        val mockEc2ClientBuilder = mock<Ec2ClientBuilder>()
        val mockEc2Client = mock<Ec2Client>()

        val customEndpoint = "http://localhost:4566"
        val properties = AwsDiscoveryProperties(
            region = "us-west-2",
            endpoint = customEndpoint,
        )

        // Setup mocks for builder pattern
        whenever(mockCredentialsProvider.getCredentials(any())).thenReturn(mockAwsCredentials)
        whenever(mockEc2ClientBuilder.region(any<Region>())).thenReturn(mockEc2ClientBuilder)
        whenever(mockEc2ClientBuilder.credentialsProvider(any<SdkCredentialsProvider>())).thenReturn(
            mockEc2ClientBuilder,
        )
        whenever(mockEc2ClientBuilder.endpointOverride(any<URI>())).thenReturn(mockEc2ClientBuilder)
        whenever(mockEc2ClientBuilder.build()).thenReturn(mockEc2Client)

        // Create provider with constructor-injected mocked dependencies
        val builderSupplier = Supplier { mockEc2ClientBuilder }
        val provider = Ec2ClientProvider(mockCredentialsProvider, builderSupplier)

        // Act
        val client = provider.getClient(properties)

        // Assert
        assertNotNull(client)
        assertEquals(mockEc2Client, client)

        // Verify custom endpoint was set
        verify(mockEc2ClientBuilder).endpointOverride(URI.create(customEndpoint))
    }
}
