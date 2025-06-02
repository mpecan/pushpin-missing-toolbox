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
import software.amazon.awssdk.services.autoscaling.AutoScalingClient
import software.amazon.awssdk.services.autoscaling.AutoScalingClientBuilder
import java.net.URI
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider as SdkCredentialsProvider

@ExtendWith(MockitoExtension::class)
class AutoScalingClientProviderTest {
    @Test
    fun `should create AutoScaling client with default settings`() {
        // Arrange
        val mockCredentialsProvider = mock<AwsCredentialsProvider>()
        val mockAwsCredentials = mock<SdkCredentialsProvider>()
        val mockAutoScalingClientBuilder = mock<AutoScalingClientBuilder>()
        val mockAutoScalingClient = mock<AutoScalingClient>()

        val properties =
            AwsDiscoveryProperties(
                region = "eu-central-1",
            )

        // Setup mocks for builder pattern
        whenever(mockCredentialsProvider.getCredentials(any())).thenReturn(mockAwsCredentials)
        whenever(mockAutoScalingClientBuilder.region(any<Region>())).thenReturn(mockAutoScalingClientBuilder)
        whenever(mockAutoScalingClientBuilder.credentialsProvider(any<SdkCredentialsProvider>())).thenReturn(
            mockAutoScalingClientBuilder,
        )
        whenever(mockAutoScalingClientBuilder.build()).thenReturn(mockAutoScalingClient)

        // Create provider with constructor-injected mocked dependencies
        val builderSupplier = Supplier { mockAutoScalingClientBuilder }
        val provider = AutoScalingClientProvider(mockCredentialsProvider, builderSupplier)

        // Act
        val client = provider.getClient(properties)

        // Assert
        assertNotNull(client)
        assertEquals(mockAutoScalingClient, client)

        // Verify correct region and credentials were set
        verify(mockAutoScalingClientBuilder).region(Region.of("eu-central-1"))
        verify(mockAutoScalingClientBuilder).credentialsProvider(mockAwsCredentials)
        verify(mockAutoScalingClientBuilder).build()
        verify(mockCredentialsProvider).getCredentials(properties)
    }

    @Test
    fun `should use custom endpoint when provided`() {
        // Arrange
        val mockCredentialsProvider = mock<AwsCredentialsProvider>()
        val mockAwsCredentials = mock<SdkCredentialsProvider>()
        val mockAutoScalingClientBuilder = mock<AutoScalingClientBuilder>()
        val mockAutoScalingClient = mock<AutoScalingClient>()

        val customEndpoint = "http://localhost:4566"
        val properties =
            AwsDiscoveryProperties(
                region = "eu-central-1",
                endpoint = customEndpoint,
            )

        // Setup mocks for builder pattern
        whenever(mockCredentialsProvider.getCredentials(any())).thenReturn(mockAwsCredentials)
        whenever(mockAutoScalingClientBuilder.region(any<Region>())).thenReturn(mockAutoScalingClientBuilder)
        whenever(mockAutoScalingClientBuilder.credentialsProvider(any<SdkCredentialsProvider>())).thenReturn(
            mockAutoScalingClientBuilder,
        )
        whenever(mockAutoScalingClientBuilder.endpointOverride(any<URI>())).thenReturn(mockAutoScalingClientBuilder)
        whenever(mockAutoScalingClientBuilder.build()).thenReturn(mockAutoScalingClient)

        // Create provider with constructor-injected mocked dependencies
        val builderSupplier = Supplier { mockAutoScalingClientBuilder }
        val provider = AutoScalingClientProvider(mockCredentialsProvider, builderSupplier)

        // Act
        val client = provider.getClient(properties)

        // Assert
        assertNotNull(client)
        assertEquals(mockAutoScalingClient, client)

        // Verify custom endpoint was set
        verify(mockAutoScalingClientBuilder).endpointOverride(URI.create(customEndpoint))
    }
}
