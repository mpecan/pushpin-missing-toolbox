package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.config.PushpinProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtChannelSubscriptionsExtractorTest {

    private lateinit var jwtChannelSubscriptionsExtractor: JwtChannelSubscriptionsExtractor
    private lateinit var mockClaimExtractor: ClaimExtractor
    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var mockJwt: Jwt

    @BeforeEach
    fun setUp() {
        mockClaimExtractor = mock()

        // Create PushpinProperties with JWT claim extraction enabled
        pushpinProperties = PushpinProperties(
            security = PushpinProperties.SecurityProperties(
                jwt = PushpinProperties.JwtProperties(
                    claimExtraction = PushpinProperties.JwtProperties.ClaimExtractionProperties(
                        enabled = true,
                        extractClaims = listOf("channels", "subscriptions")
                    )
                )
            )
        )

        jwtChannelSubscriptionsExtractor = JwtChannelSubscriptionsExtractor(mockClaimExtractor, pushpinProperties)

        // Create a mock JWT token
        val headers = mapOf(
            "alg" to "HS256",
            "typ" to "JWT"
        )

        val claims = mapOf(
            "sub" to "testuser"
        )

        mockJwt = Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            headers,
            claims
        )
    }

    @Test
    fun `extractChannelSubscriptions should return null when claim extraction is disabled`() {
        // Arrange
        val propertiesWithDisabledExtraction = PushpinProperties(
            security = PushpinProperties.SecurityProperties(
                jwt = PushpinProperties.JwtProperties(
                    claimExtraction = PushpinProperties.JwtProperties.ClaimExtractionProperties(
                        enabled = false
                    )
                )
            )
        )

        val extractor = JwtChannelSubscriptionsExtractor(mockClaimExtractor, propertiesWithDisabledExtraction)

        // Act
        val result = extractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNull(result)
    }

    @Test
    fun `extractChannelSubscriptions should extract array of channel IDs`() {
        // Arrange
        val channelsList = listOf("channel1", "channel2", "news.*")
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(channelsList)
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(emptyMap())

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNotNull(result)
        assertEquals("testuser", result.principal)
        assertEquals(3, result.subscriptions.size)
        assertEquals(false, result.defaultAllow)

        val channelIds = result.subscriptions.map { it.channelId }
        assertEquals(listOf("channel1", "channel2", "news.*"), channelIds)

        result.subscriptions.forEach { subscription ->
            assertEquals(true, subscription.allowed)
            assertEquals(emptyMap(), subscription.metadata)
        }
    }

    @Test
    fun `extractChannelSubscriptions should extract map of channel IDs to metadata`() {
        // Arrange
        val channelsMap = mapOf(
            "channel1" to mapOf("expires" to "2024-12-31"),
            "channel2" to emptyMap<String, Any>()
        )
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(emptyList())
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(channelsMap)

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNotNull(result)
        assertEquals("testuser", result.principal)
        assertEquals(2, result.subscriptions.size)
        assertEquals(false, result.defaultAllow)

        val channel1 = result.subscriptions.find { it.channelId == "channel1" }
        assertNotNull(channel1)
        assertEquals(true, channel1.allowed)
        assertEquals(mapOf("expires" to "2024-12-31"), channel1.metadata)

        val channel2 = result.subscriptions.find { it.channelId == "channel2" }
        assertNotNull(channel2)
        assertEquals(true, channel2.allowed)
        assertEquals(emptyMap(), channel2.metadata)
    }

    @Test
    fun `extractChannelSubscriptions should extract array of channel objects`() {
        // Arrange
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(emptyList())
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(emptyMap())

        // Mock the behavior for array of objects format
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[0]"))
            .thenReturn(mapOf("id" to "channel1", "expires" to "2024-12-31"))
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[1]"))
            .thenReturn(mapOf("id" to "channel2"))
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[2]"))
            .thenReturn(emptyMap())

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNotNull(result)
        assertEquals("testuser", result.principal)
        assertEquals(2, result.subscriptions.size)
        assertEquals(false, result.defaultAllow)

        val channel1 = result.subscriptions.find { it.channelId == "channel1" }
        assertNotNull(channel1)
        assertEquals(true, channel1.allowed)
        assertEquals(mapOf("expires" to "2024-12-31"), channel1.metadata)

        val channel2 = result.subscriptions.find { it.channelId == "channel2" }
        assertNotNull(channel2)
        assertEquals(true, channel2.allowed)
        assertEquals(emptyMap(), channel2.metadata)
    }

    @Test
    fun `extractChannelSubscriptions should handle alternative channel ID field names`() {
        // Arrange
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(emptyList())
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(emptyMap())

        // Mock the behavior for array of objects with different ID field names
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[0]"))
            .thenReturn(mapOf("channelId" to "channel1", "expires" to "2024-12-31"))
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[1]"))
            .thenReturn(mapOf("channel" to "channel2"))
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[2]"))
            .thenReturn(emptyMap())

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNotNull(result)
        assertEquals(2, result.subscriptions.size)

        val channelIds = result.subscriptions.map { it.channelId }
        assertEquals(listOf("channel1", "channel2"), channelIds)
    }

    @Test
    fun `extractChannelSubscriptions should return null when no channels are found`() {
        // Arrange
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(emptyList())
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(emptyMap())
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, "$claimPath[0]")).thenReturn(emptyMap())

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNull(result)
    }

    @Test
    fun `extractChannelSubscriptions should handle exceptions gracefully`() {
        // Arrange
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath))
            .thenThrow(RuntimeException("Test exception"))

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNull(result)
    }

    @Test
    fun `extractChannelSubscriptions should use subscription claim path if available`() {
        // Arrange
        val channelsList = listOf("channel1", "channel2")

        // Create a custom PushpinProperties with only "subscriptions" in the extractClaims list
        val customProperties = PushpinProperties(
            security = PushpinProperties.SecurityProperties(
                jwt = PushpinProperties.JwtProperties(
                    claimExtraction = PushpinProperties.JwtProperties.ClaimExtractionProperties(
                        enabled = true,
                        extractClaims = listOf("subscriptions") // Only subscriptions, no channels
                    )
                )
            )
        )

        // Create a new extractor with the custom properties
        val customExtractor = JwtChannelSubscriptionsExtractor(mockClaimExtractor, customProperties)

        // The channelsClaimPath should now be "subscriptions"
        val claimPath = customExtractor.channelsClaimPath
        assertEquals("subscriptions", claimPath)

        // Mock the behavior for the subscriptions path
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(channelsList)
        whenever(mockClaimExtractor.extractMapClaim(mockJwt, claimPath)).thenReturn(emptyMap())

        // Act
        val result = customExtractor.extractChannelSubscriptions(mockJwt)

        // Assert
        assertNotNull(result)
        assertEquals(2, result.subscriptions.size)

        val channelIds = result.subscriptions.map { it.channelId }
        assertEquals(listOf("channel1", "channel2"), channelIds)
    }

    @Test
    fun `extractChannelSubscriptions should use anonymous as principal when subject is null`() {
        // Arrange
        val channelsList = listOf("channel1")
        val claimPath = jwtChannelSubscriptionsExtractor.channelsClaimPath
        whenever(mockClaimExtractor.extractListClaim(mockJwt, claimPath)).thenReturn(channelsList)

        // Create JWT with null subject
        val jwtWithNullSubject = Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            mapOf("alg" to "HS256", "typ" to "JWT"),
            mapOf("name" to "Test User") // No "sub" claim
        )

        // Also need to mock for the new JWT
        whenever(mockClaimExtractor.extractListClaim(jwtWithNullSubject, claimPath)).thenReturn(channelsList)

        // Act
        val result = jwtChannelSubscriptionsExtractor.extractChannelSubscriptions(jwtWithNullSubject)

        // Assert
        assertNotNull(result)
        assertEquals("anonymous", result.principal)
    }
}
