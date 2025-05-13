package io.github.mpecan.pmt.security.model

/**
 * Represents permissions for a specific channel.
 *
 * @property channelId The ID of the channel
 * @property permissions The permissions for the channel
 */
data class ChannelPermissions(
    val channelId: String,
    val permissions: Set<ChannelPermission>
) {
    /**
     * Check if the permissions include admin access.
     */
    fun hasAdminPermission(): Boolean {
        return permissions.contains(ChannelPermission.ADMIN)
    }
    
    /**
     * Check if the permissions include write access.
     */
    fun hasWritePermission(): Boolean {
        return permissions.contains(ChannelPermission.WRITE) || hasAdminPermission()
    }
    
    /**
     * Check if the permissions include read access.
     */
    fun hasReadPermission(): Boolean {
        return permissions.contains(ChannelPermission.READ) || hasWritePermission()
    }
}