package io.github.mpecan.pmt.security.model

/**
 * Represents a channel subscription authorization result.
 * This is used to communicate which channels a user can subscribe to
 * and any metadata associated with the subscription.
 *
 * @property channelId The ID of the channel
 * @property allowed Whether the subscription is allowed
 * @property metadata Optional metadata about the subscription (e.g., expiry time, filters)
 */
data class ChannelSubscription(
    val channelId: String,
    val allowed: Boolean = true,
    val metadata: Map<String, Any> = emptyMap(),
) {
    /**
     * Get metadata value with type safety.
     */
    inline fun <reified T> getMetadata(key: String): T? {
        return metadata[key] as? T
    }
}

/**
 * Represents a collection of channel subscriptions for a principal.
 * * @property principal The principal (user) these subscriptions belong to
 * @property subscriptions The list of channel subscriptions
 * @property defaultAllow Whether to allow subscription to channels not explicitly listed
 */
data class ChannelSubscriptions(
    val principal: String,
    val subscriptions: List<ChannelSubscription>,
    val defaultAllow: Boolean = false,
) {
    /**
     * Check if subscription to a specific channel is allowed.
     */
    fun canSubscribe(channelId: String): Boolean {
        val subscription = subscriptions.find { it.channelId == channelId }
        return subscription?.allowed ?: defaultAllow
    }

    /**
     * Get all allowed channel IDs.
     */
    fun getAllowedChannels(): List<String> {
        return subscriptions.filter { it.allowed }.map { it.channelId }
    }
}
