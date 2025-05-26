package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.model.ChannelPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtChannelPermissionsExtractorTest {

    private lateinit var claimExtractor: ClaimExtractor
    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var jwtChannelPermissionsExtractor: JwtChannelPermissionsExtractor
    private lateinit var mockJwt: Jwt

    @BeforeEach
    fun setUp() {
        claimExtractor = mock()
        
        // Create mock properties with claim extraction enabled
        val claimExtractionProperties = mock<PushpinProperties.JwtProperties.ClaimExtractionProperties> {
            on { enabled } doReturn true
            on { extractClaims } doReturn listOf("channels", "permissions", "flatChannels")
        }
        
        val jwtProperties = mock<PushpinProperties.JwtProperties> {
            on { claimExtraction } doReturn claimExtractionProperties
        }
        
        val securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { jwt } doReturn jwtProperties
        }
        
        pushpinProperties = mock {
            on { security } doReturn securityProperties
        }
        
        jwtChannelPermissionsExtractor = JwtChannelPermissionsExtractor(claimExtractor, pushpinProperties)
        
        // Create mock JWT for testing
        mockJwt = Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            mapOf("alg" to "HS256"),
            mapOf("sub" to "testuser")
        )
    }

    @Test
    fun `extractChannelPermissions should return empty list when claim extraction is disabled`() {
        // Arrange
        whenever(pushpinProperties.security.jwt.claimExtraction.enabled).thenReturn(false)
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `extractChannelPermissions should parse map format`() {
        // Arrange
        val channelsMap = mapOf(
            "channel1" to listOf("READ", "WRITE"),
            "channel2" to listOf("READ")
        )
        
        whenever(claimExtractor.extractMapClaim(any(), any())).thenReturn(channelsMap)
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(2, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(1, channel2Perms?.permissions?.size)
        assertTrue(channel2Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
    }
    
    @Test
    fun `extractChannelPermissions should handle string permission values`() {
        // Arrange
        val channelsMap = mapOf(
            "channel1" to "READ,WRITE",
            "channel2" to "READ"
        )
        
        whenever(claimExtractor.extractMapClaim(any(), any())).thenReturn(channelsMap)
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(2, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(1, channel2Perms?.permissions?.size)
        assertTrue(channel2Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
    }
    
    @Test
    fun `extractChannelPermissions should parse object array format`() {
        // Arrange
        // First return empty map to trigger array format check
        whenever(claimExtractor.extractMapClaim(any(), eq("channels"))).thenReturn(emptyMap())
        
        // Return array of channel strings
        whenever(claimExtractor.extractListClaim(any(), eq("channels"))).thenReturn(listOf("dummy"))
        
        // Setup mock for the first channel object
        val channel1Object = mapOf(
            "id" to "channel1",
            "permissions" to listOf("READ", "WRITE")
        )
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[0]"))).thenReturn(channel1Object)
        
        // Setup mock for the second channel object
        val channel2Object = mapOf(
            "id" to "channel2",
            "permissions" to listOf("READ")
        )
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[1]"))).thenReturn(channel2Object)
        
        // Setup mock for the third channel object (empty to terminate loop)
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[2]"))).thenReturn(emptyMap())
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(2, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(1, channel2Perms?.permissions?.size)
        assertTrue(channel2Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
    }
    
    @Test
    fun `extractChannelPermissions should support channelId instead of id`() {
        // Arrange
        // First return empty map to trigger array format check
        whenever(claimExtractor.extractMapClaim(any(), eq("channels"))).thenReturn(emptyMap())
        
        // Return array of channel strings
        whenever(claimExtractor.extractListClaim(any(), eq("channels"))).thenReturn(listOf("dummy"))
        
        // Setup mock for the channel object with channelId instead of id
        val channelObject = mapOf(
            "channelId" to "channel1",
            "permissions" to listOf("READ", "WRITE")
        )
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[0]"))).thenReturn(channelObject)
        
        // Setup mock for the second channel object (empty to terminate loop)
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[1]"))).thenReturn(emptyMap())
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(1, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
    }
    
    @Test
    fun `extractChannelPermissions should parse flat channel strings format`() {
        // Arrange
        // First return empty map to trigger array format check
        whenever(claimExtractor.extractMapClaim(any(), eq("channels"))).thenReturn(emptyMap())
        
        // Return array of channel strings
        val channelStrings = listOf(
            "channel1:READ",
            "channel1:WRITE",
            "channel2:READ"
        )
        whenever(claimExtractor.extractListClaim(any(), any())).thenReturn(channelStrings)
        
        // Make all map queries to nested objects return empty to go to string parsing
        whenever(claimExtractor.extractMapClaim(any(), eq("channels[0]"))).thenReturn(emptyMap())
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(2, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(1, channel2Perms?.permissions?.size)
        assertTrue(channel2Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
    }
    
    @Test
    fun `extractChannelPermissions should handle invalid permission values`() {
        // Arrange
        val channelsMap = mapOf(
            "channel1" to listOf("READ", "INVALID", "WRITE"),
            "channel2" to listOf("INVALID")
        )
        
        whenever(claimExtractor.extractMapClaim(any(), any())).thenReturn(channelsMap)
        
        // Act
        val result = jwtChannelPermissionsExtractor.extractChannelPermissions(mockJwt)
        
        // Assert
        assertEquals(1, result.size) // Only channel1 has valid permissions
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        // channel2 should not be included as it has no valid permissions
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(null, channel2Perms)
    }
}