package io.github.mpecan.pmt.security.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChannelPermissionsTest {
    
    @Test
    fun `should correctly check admin permission`() {
        val permissions = ChannelPermissions(
            channelId = "test-channel",
            permissions = setOf(ChannelPermission.ADMIN)
        )
        
        assertThat(permissions.hasAdminPermission()).isTrue()
        assertThat(permissions.hasWritePermission()).isTrue()
        assertThat(permissions.hasReadPermission()).isTrue()
    }
    
    @Test
    fun `should correctly check write permission`() {
        val permissions = ChannelPermissions(
            channelId = "test-channel",
            permissions = setOf(ChannelPermission.WRITE)
        )
        
        assertThat(permissions.hasAdminPermission()).isFalse()
        assertThat(permissions.hasWritePermission()).isTrue()
        assertThat(permissions.hasReadPermission()).isTrue()
    }
    
    @Test
    fun `should correctly check read permission`() {
        val permissions = ChannelPermissions(
            channelId = "test-channel",
            permissions = setOf(ChannelPermission.READ)
        )
        
        assertThat(permissions.hasAdminPermission()).isFalse()
        assertThat(permissions.hasWritePermission()).isFalse()
        assertThat(permissions.hasReadPermission()).isTrue()
    }
    
    @Test
    fun `should handle empty permissions`() {
        val permissions = ChannelPermissions(
            channelId = "test-channel",
            permissions = emptySet()
        )
        
        assertThat(permissions.hasAdminPermission()).isFalse()
        assertThat(permissions.hasWritePermission()).isFalse()
        assertThat(permissions.hasReadPermission()).isFalse()
    }
    
    @Test
    fun `should handle multiple permissions`() {
        val permissions = ChannelPermissions(
            channelId = "test-channel",
            permissions = setOf(ChannelPermission.READ, ChannelPermission.WRITE)
        )
        
        assertThat(permissions.hasAdminPermission()).isFalse()
        assertThat(permissions.hasWritePermission()).isTrue()
        assertThat(permissions.hasReadPermission()).isTrue()
    }
}