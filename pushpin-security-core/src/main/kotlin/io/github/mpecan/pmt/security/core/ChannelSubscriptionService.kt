package io.github.mpecan.pmt.security.core

/**
 * Core interface for channel subscription authorization.
 * Implementations determine which channels a user/principal can subscribe to
 * for receiving real-time updates through Pushpin.
 */
interface ChannelSubscriptionService {
    /**
     * Check if a principal can subscribe to a specific channel.
     *
     * @param principal The authenticated principal (user, service, etc.)
     * @param channelId The channel ID to check subscription access for
     * @return True if the principal can subscribe to the channel, false otherwise
     */
    fun canSubscribe(principal: Any?, channelId: String): Boolean
    
    /**
     * Get all channels that the principal can subscribe to.
     *
     * @param principal The authenticated principal
     * @return List of channel IDs that the principal can subscribe to
     */
    fun getSubscribableChannels(principal: Any?): List<String>
    
    /**
     * Get channels matching a pattern that the principal can subscribe to.
     * This is useful for wildcard subscriptions or prefix-based access.
     *
     * @param principal The authenticated principal
     * @param pattern The channel pattern (e.g., "news.*", "user.123.*")
     * @return List of channel IDs matching the pattern that the principal can subscribe to
     */
    fun getSubscribableChannelsByPattern(principal: Any?, pattern: String): List<String> = emptyList()
}