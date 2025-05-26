package io.github.mpecan.pmt.security.remote

import jakarta.servlet.http.HttpServletRequest

/**
 * Interface for remote authorization API client.
 * This client is responsible for checking if a user can subscribe to channels
 * by calling a remote authorization service.
 */
interface RemoteAuthorizationClient {
    /**
     * Check if a user can subscribe to a specific channel.
     *
     * @param request The HTTP request containing authentication details
     * @param channelId The channel ID to check subscription access for
     * @return True if user can subscribe to the channel, false otherwise
     */
    fun canSubscribe(request: HttpServletRequest, channelId: String): Boolean
    
    /**
     * Get all channels that the user can subscribe to.
     *
     * @param request The HTTP request containing authentication details
     * @return List of channel IDs that the user can subscribe to
     */
    fun getSubscribableChannels(request: HttpServletRequest): List<String>
    
    /**
     * Get channels matching a pattern that the user can subscribe to.
     *
     * @param request The HTTP request containing authentication details
     * @param pattern The channel pattern (e.g., "news.*", "user.123.*")
     * @return List of channel IDs matching the pattern that the user can subscribe to
     */
    fun getSubscribableChannelsByPattern(request: HttpServletRequest, pattern: String): List<String> = emptyList()
}