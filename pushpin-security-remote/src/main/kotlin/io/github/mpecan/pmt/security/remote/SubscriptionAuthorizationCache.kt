package io.github.mpecan.pmt.security.remote

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * Cache for channel subscription authorization decisions to reduce load on remote authorization service.
 */
class SubscriptionAuthorizationCache(
    cacheMaxSize: Long = 10000,
    cacheTtl: Long = 300000, // 5 minutes default
) {

    private val cache: Cache<CacheKey, CacheValue> = Caffeine.newBuilder()
        .maximumSize(cacheMaxSize)
        .expireAfterWrite(cacheTtl, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Check if a subscription check is cached.
     *
     * @param userId User ID to check
     * @param channelId Channel ID to check
     * @return True if the user can subscribe to the channel, false otherwise, or null if not cached
     */
    fun getSubscriptionCheck(userId: String, channelId: String): Boolean? {
        val key = CacheKey.forSubscriptionCheck(userId, channelId)
        val value = cache.getIfPresent(key)
        return value?.boolValue
    }

    /**
     * Cache a subscription check result.
     *
     * @param userId User ID
     * @param channelId Channel ID
     * @param canSubscribe Result of the subscription check
     */
    fun cacheSubscriptionCheck(userId: String, channelId: String, canSubscribe: Boolean) {
        val key = CacheKey.forSubscriptionCheck(userId, channelId)
        cache.put(key, CacheValue.ofBoolean(canSubscribe))
    }

    /**
     * Get cached subscribable channels for a user.
     *
     * @param userId User ID
     * @return List of channel IDs, or null if not cached
     */
    fun getSubscribableChannels(userId: String): List<String>? {
        val key = CacheKey.forChannelsList(userId)
        val value = cache.getIfPresent(key)
        return value?.listValue
    }

    /**
     * Cache subscribable channels for a user.
     *
     * @param userId User ID
     * @param channels List of channel IDs
     */
    fun cacheSubscribableChannels(userId: String, channels: List<String>) {
        val key = CacheKey.forChannelsList(userId)
        cache.put(key, CacheValue.ofList(channels))
    }

    /**
     * Get cached subscribable channels by pattern.
     *
     * @param userId User ID
     * @param pattern Channel pattern
     * @return List of channel IDs, or null if not cached
     */
    fun getSubscribableChannelsByPattern(userId: String, pattern: String): List<String>? {
        val key = CacheKey.forPatternChannels(userId, pattern)
        val value = cache.getIfPresent(key)
        return value?.listValue
    }

    /**
     * Cache subscribable channels by pattern.
     *
     * @param userId User ID
     * @param pattern Channel pattern
     * @param channels List of channel IDs
     */
    fun cacheSubscribableChannelsByPattern(userId: String, pattern: String, channels: List<String>) {
        val key = CacheKey.forPatternChannels(userId, pattern)
        cache.put(key, CacheValue.ofList(channels))
    }

    /**
     * Clear all cache entries for a user.
     */
    @Suppress("unused")
    fun clearUserCache(userId: String) {
        cache.asMap().keys.removeIf { it.userId == userId }
    }

    /**
     * Key for cache entries.
     */
    private data class CacheKey(
        val type: CacheType,
        val userId: String,
        val channelId: String? = null,
        val pattern: String? = null,
    ) {
        companion object {
            fun forSubscriptionCheck(userId: String, channelId: String): CacheKey {
                return CacheKey(CacheType.SUBSCRIPTION_CHECK, userId, channelId = channelId)
            }

            fun forChannelsList(userId: String): CacheKey {
                return CacheKey(CacheType.CHANNELS_LIST, userId)
            }

            fun forPatternChannels(userId: String, pattern: String): CacheKey {
                return CacheKey(CacheType.PATTERN_CHANNELS, userId, pattern = pattern)
            }
        }
    }

    /**
     * Type of cache entry.
     */
    private enum class CacheType {
        SUBSCRIPTION_CHECK,
        CHANNELS_LIST,
        PATTERN_CHANNELS,
    }

    /**
     * Value for cache entries.
     */
    private data class CacheValue(
        val boolValue: Boolean? = null,
        val listValue: List<String>? = null,
    ) {
        companion object {
            fun ofBoolean(value: Boolean): CacheValue {
                return CacheValue(boolValue = value)
            }

            fun ofList(value: List<String>): CacheValue {
                return CacheValue(listValue = value)
            }
        }
    }
}
