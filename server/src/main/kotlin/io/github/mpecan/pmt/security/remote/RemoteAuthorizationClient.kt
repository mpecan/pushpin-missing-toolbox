package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import jakarta.servlet.http.HttpServletRequest

/**
 * Interface for remote authorization API client.
 * This client is responsible for checking if a user has permissions to access a channel
 * by calling a remote authorization service.
 */
interface RemoteAuthorizationClient {
    /**
     * Check if a user has the required permission for a specific channel.
     *
     * @param request The HTTP request containing authentication details
     * @param channelId The channel ID to check permissions for
     * @param permission The required permission
     * @return True if user has the required permission, false otherwise
     */
    fun hasPermission(request: HttpServletRequest, channelId: String, permission: ChannelPermission): Boolean
    
    /**
     * Get all channels that the user has the specified permission for.
     *
     * @param request The HTTP request containing authentication details
     * @param permission The required permission
     * @return List of channel IDs that the user has the specified permission for
     */
    fun getChannelsWithPermission(request: HttpServletRequest, permission: ChannelPermission): List<String>
    
    /**
     * Get all channel permissions for the user.
     *
     * @param request The HTTP request containing authentication details
     * @return List of ChannelPermissions objects containing channel IDs and their permissions
     */
    fun getAllChannelPermissions(request: HttpServletRequest): List<ChannelPermissions>
}