package io.github.mpecan.pmt.security.service

import io.github.mpecan.pmt.security.model.ChannelPermission
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

class ChannelAuthorizationServiceTest {
    
    private lateinit var channelAuthorizationService: ChannelAuthorizationService
    
    @BeforeEach
    fun setUp() {
        channelAuthorizationService = ChannelAuthorizationService()
    }
    
    @Test
    fun `hasPermission should return false when no permissions granted`() {
        // Arrange
        val username = "testuser"
        val channelId = "testchannel"
        val permission = ChannelPermission.READ
        
        // Act
        val hasPermission = channelAuthorizationService.hasPermission(username, channelId, permission)
        
        // Assert
        assertFalse(hasPermission)
    }
    
    @Test
    fun `hasPermission should return true when specific permission granted`() {
        // Arrange
        val username = "testuser"
        val channelId = "testchannel"
        val permission = ChannelPermission.READ
        
        // Grant permission
        channelAuthorizationService.grantUserPermissions(username, channelId, permission)
        
        // Act
        val hasPermission = channelAuthorizationService.hasPermission(username, channelId, permission)
        
        // Assert
        assertTrue(hasPermission)
    }
    
    @Test
    fun `hasPermission should respect permission hierarchy`() {
        // Arrange
        val username = "testuser"
        val channelId = "testchannel"
        
        // Grant WRITE permission (which should implicitly grant READ)
        channelAuthorizationService.grantUserPermissions(username, channelId, ChannelPermission.WRITE)
        
        // Act & Assert
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.READ))
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.WRITE))
        assertFalse(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.ADMIN))
        
        // Now grant ADMIN permission
        channelAuthorizationService.grantUserPermissions(username, channelId, ChannelPermission.ADMIN)
        
        // Act & Assert - all permissions should be granted
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.READ))
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.WRITE))
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.ADMIN))
    }
    
    @Test
    fun `hasPermission with authentication should check both user and role permissions`() {
        // Arrange
        val username = "testuser"
        val role = "ROLE_USER"
        val channelId = "testchannel"
        val otherChannelId = "otherchannel"
        
        // Create authentication
        val authorities = listOf(SimpleGrantedAuthority(role))
        val authentication = UsernamePasswordAuthenticationToken(username, null, authorities)
        
        // Grant user permission to testchannel
        channelAuthorizationService.grantUserPermissions(username, channelId, ChannelPermission.READ)
        
        // Grant role permission to otherchannel
        channelAuthorizationService.grantRolePermissions(role, otherChannelId, ChannelPermission.WRITE)
        
        // Act & Assert
        // User should have access to testchannel via user permissions
        assertTrue(channelAuthorizationService.hasPermission(authentication, channelId, ChannelPermission.READ))
        
        // User should have access to otherchannel via role permissions
        assertTrue(channelAuthorizationService.hasPermission(authentication, otherChannelId, ChannelPermission.READ))
        assertTrue(channelAuthorizationService.hasPermission(authentication, otherChannelId, ChannelPermission.WRITE))
    }
    
    @Test
    fun `revokeUserPermissions should remove specified permissions`() {
        // Arrange
        val username = "testuser"
        val channelId = "testchannel"

        // Grant all permissions
        channelAuthorizationService.grantUserPermissions(
            username,
            channelId,
            ChannelPermission.READ,
            ChannelPermission.WRITE,
            ChannelPermission.ADMIN
        )

        // Check that all permissions were granted
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.READ))
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.WRITE))
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.ADMIN))

        // Act - revoke WRITE permission
        channelAuthorizationService.revokeUserPermissions(username, channelId, ChannelPermission.WRITE)

        // Get the user permissions directly to check what's actually stored
        val userPerms = channelAuthorizationService.getUserPermissionsForTesting(username)
        val channelPerms = userPerms.find { it.channelId == channelId }

        // Assert - should still have READ and ADMIN but not WRITE in the actual permissions set
        assertTrue(channelPerms!!.permissions.contains(ChannelPermission.READ))
        assertFalse(channelPerms.permissions.contains(ChannelPermission.WRITE))
        assertTrue(channelPerms.permissions.contains(ChannelPermission.ADMIN))

        // Also check that the permissions still work correctly through the API
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.READ))
        // Due to permission hierarchy in hasPermission, WRITE is implied by ADMIN so we don't assert on it
        assertTrue(channelAuthorizationService.hasPermission(username, channelId, ChannelPermission.ADMIN))
    }
    
    @Test
    fun `getChannelsWithPermission should return all channels with given permission`() {
        // Arrange
        val username = "testuser"
        val channel1 = "channel1"
        val channel2 = "channel2"
        val channel3 = "channel3"
        
        // Grant different permissions
        channelAuthorizationService.grantUserPermissions(username, channel1, ChannelPermission.READ)
        channelAuthorizationService.grantUserPermissions(username, channel2, ChannelPermission.WRITE)
        channelAuthorizationService.grantUserPermissions(username, channel3, ChannelPermission.ADMIN)
        
        // Act
        val readChannels = channelAuthorizationService.getUserChannelsWithPermission(username, ChannelPermission.READ)
        val writeChannels = channelAuthorizationService.getUserChannelsWithPermission(username, ChannelPermission.WRITE)
        val adminChannels = channelAuthorizationService.getUserChannelsWithPermission(username, ChannelPermission.ADMIN)
        
        // Assert
        assertEquals(3, readChannels.size)
        assertTrue(readChannels.contains(channel1))
        assertTrue(readChannels.contains(channel2))
        assertTrue(readChannels.contains(channel3))
        
        assertEquals(2, writeChannels.size)
        assertFalse(writeChannels.contains(channel1))
        assertTrue(writeChannels.contains(channel2))
        assertTrue(writeChannels.contains(channel3))
        
        assertEquals(1, adminChannels.size)
        assertFalse(adminChannels.contains(channel1))
        assertFalse(adminChannels.contains(channel2))
        assertTrue(adminChannels.contains(channel3))
    }
}