package io.github.mpecan.pmt.security.core

import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions

/**
 * Core interface for authorization services.
 * Implementations can provide different authorization strategies such as JWT-based,
 * remote API-based, or custom authorization logic.
 */
interface AuthorizationService {
    /**
     * Check if a request has the required permission for a specific channel.
     *
     * @param principal The authenticated principal (user, service, etc.)
     * @param channelId The channel ID to check permissions for
     * @param permission The required permission
     * @return True if the principal has the required permission, false otherwise
     */
    fun hasPermission(principal: Any?, channelId: String, permission: ChannelPermission): Boolean
    
    /**
     * Get all channels that the principal has the specified permission for.
     *
     * @param principal The authenticated principal
     * @param permission The required permission
     * @return List of channel IDs that the principal has the specified permission for
     */
    fun getChannelsWithPermission(principal: Any?, permission: ChannelPermission): List<String>
    
    /**
     * Get all channel permissions for the principal.
     *
     * @param principal The authenticated principal
     * @return List of ChannelPermissions objects containing channel IDs and their permissions
     */
    fun getAllChannelPermissions(principal: Any?): List<ChannelPermissions>
}