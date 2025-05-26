package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthorizationCacheTest {

    private lateinit var authorizationCache: AuthorizationCache
    private lateinit var pushpinProperties: PushpinProperties

    @BeforeEach
    fun setUp() {
        // Create mock properties with caching configuration
        val remoteAuthProperties = mock<PushpinProperties.JwtProperties.RemoteAuthorizationProperties> {
            on { cacheEnabled } doReturn true
            on { cacheTtl } doReturn 300000L // 5 minutes
            on { cacheMaxSize } doReturn 1000L
        }
        
        val jwtProperties = mock<PushpinProperties.JwtProperties> {
            on { remoteAuthorization } doReturn remoteAuthProperties
        }
        
        val securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { jwt } doReturn jwtProperties
        }
        
        pushpinProperties = mock {
            on { security } doReturn securityProperties
        }
        
        authorizationCache = AuthorizationCache(pushpinProperties)
    }

    @Test
    fun `getPermissionCheck should return null for non-cached items`() {
        // Act
        val result = authorizationCache.getPermissionCheck("user1", "channel1", ChannelPermission.READ)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `cachePermissionCheck should store permission check results`() {
        // Arrange
        val userId = "user1"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        val hasPermission = true
        
        // Act
        authorizationCache.cachePermissionCheck(userId, channelId, permission, hasPermission)
        val result = authorizationCache.getPermissionCheck(userId, channelId, permission)
        
        // Assert
        assertEquals(hasPermission, result)
    }
    
    @Test
    fun `getChannelsWithPermission should return null for non-cached items`() {
        // Act
        val result = authorizationCache.getChannelsWithPermission("user1", ChannelPermission.READ)
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `cacheChannelsWithPermission should store channel lists`() {
        // Arrange
        val userId = "user1"
        val permission = ChannelPermission.READ
        val channels = listOf("channel1", "channel2")
        
        // Act
        authorizationCache.cacheChannelsWithPermission(userId, permission, channels)
        val result = authorizationCache.getChannelsWithPermission(userId, permission)
        
        // Assert
        assertEquals(channels, result)
    }
    
    @Test
    fun `getAllChannelPermissions should return null for non-cached items`() {
        // Act
        val result = authorizationCache.getAllChannelPermissions("user1")
        
        // Assert
        assertNull(result)
    }
    
    @Test
    fun `cacheAllChannelPermissions should store permission lists`() {
        // Arrange
        val userId = "user1"
        val permissions = listOf(
            ChannelPermissions("channel1", setOf(ChannelPermission.READ, ChannelPermission.WRITE)),
            ChannelPermissions("channel2", setOf(ChannelPermission.READ))
        )
        
        // Act
        authorizationCache.cacheAllChannelPermissions(userId, permissions)
        val result = authorizationCache.getAllChannelPermissions(userId)
        
        // Assert
        assertEquals(permissions, result)
    }
    
    @Test
    fun `cache entries should be unique by user, channel, and permission`() {
        // Arrange
        val userId = "user1"
        val channelId = "channel1"
        
        // Act - cache different permission checks
        authorizationCache.cachePermissionCheck(userId, channelId, ChannelPermission.READ, true)
        authorizationCache.cachePermissionCheck(userId, channelId, ChannelPermission.WRITE, false)
        
        // Assert - each should have its own value
        assertEquals(true, authorizationCache.getPermissionCheck(userId, channelId, ChannelPermission.READ))
        assertEquals(false, authorizationCache.getPermissionCheck(userId, channelId, ChannelPermission.WRITE))
    }
    
    @Test
    fun `cache entries should be unique by user`() {
        // Arrange
        val user1 = "user1"
        val user2 = "user2"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        
        // Act - cache different user permissions
        authorizationCache.cachePermissionCheck(user1, channelId, permission, true)
        authorizationCache.cachePermissionCheck(user2, channelId, permission, false)
        
        // Assert - each should have its own value
        assertEquals(true, authorizationCache.getPermissionCheck(user1, channelId, permission))
        assertEquals(false, authorizationCache.getPermissionCheck(user2, channelId, permission))
    }
    
    @Test
    fun `cache entries should be unique by channel`() {
        // Arrange
        val userId = "user1"
        val channel1 = "channel1"
        val channel2 = "channel2"
        val permission = ChannelPermission.READ
        
        // Act - cache different channel permissions
        authorizationCache.cachePermissionCheck(userId, channel1, permission, true)
        authorizationCache.cachePermissionCheck(userId, channel2, permission, false)
        
        // Assert - each should have its own value
        assertEquals(true, authorizationCache.getPermissionCheck(userId, channel1, permission))
        assertEquals(false, authorizationCache.getPermissionCheck(userId, channel2, permission))
    }
}