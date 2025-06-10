package io.github.mpecan.pmt.security.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubscriptionAuthorizationCacheTest {
    private lateinit var cache: SubscriptionAuthorizationCache

    @BeforeEach
    fun setUp() {
        cache =
            SubscriptionAuthorizationCache(
                cacheMaxSize = 100L,
                cacheTtl = 300000L, // 5 minutes
            )
    }

    @Test
    fun `should store and retrieve cached subscription check result`() {
        val userId = "user123"
        val channelId = "test-channel"
        val authorized = true

        cache.cacheSubscriptionCheck(userId, channelId, authorized)
        val result = cache.getSubscriptionCheck(userId, channelId)

        assertEquals(authorized, result)
    }

    @Test
    fun `should return null for subscription check cache miss`() {
        val result = cache.getSubscriptionCheck("user123", "non-existent-channel")
        assertNull(result)
    }

    @Test
    fun `should update cached subscription check result`() {
        val userId = "user123"
        val channelId = "test-channel"

        cache.cacheSubscriptionCheck(userId, channelId, true)
        val firstResult = cache.getSubscriptionCheck(userId, channelId)

        cache.cacheSubscriptionCheck(userId, channelId, false)
        val secondResult = cache.getSubscriptionCheck(userId, channelId)

        assertTrue(firstResult!!)
        assertFalse(secondResult!!)
    }

    @Test
    fun `should handle different users with same channel separately`() {
        val channelId = "same-channel"

        cache.cacheSubscriptionCheck("user1", channelId, true)
        cache.cacheSubscriptionCheck("user2", channelId, false)

        assertTrue(cache.getSubscriptionCheck("user1", channelId)!!)
        assertFalse(cache.getSubscriptionCheck("user2", channelId)!!)
    }

    @Test
    fun `should store and retrieve cached subscribable channels`() {
        val userId = "user123"
        val channels = listOf("channel1", "channel2", "channel3")

        cache.cacheSubscribableChannels(userId, channels)
        val result = cache.getSubscribableChannels(userId)

        assertEquals(channels, result)
    }

    @Test
    fun `should return null for subscribable channels cache miss`() {
        val result = cache.getSubscribableChannels("non-existent-user")
        assertNull(result)
    }

    @Test
    fun `should store and retrieve cached subscribable channels by pattern`() {
        val userId = "user123"
        val pattern = "news.*"
        val channels = listOf("news.sports", "news.tech")

        cache.cacheSubscribableChannelsByPattern(userId, pattern, channels)
        val result = cache.getSubscribableChannelsByPattern(userId, pattern)

        assertEquals(channels, result)
    }

    @Test
    fun `should return null for subscribable channels by pattern cache miss`() {
        val result = cache.getSubscribableChannelsByPattern("user123", "non-existent-pattern")
        assertNull(result)
    }

    @Test
    fun `should handle empty channel id in subscription check`() {
        val userId = "user123"
        val channelId = ""

        cache.cacheSubscriptionCheck(userId, channelId, true)
        val result = cache.getSubscriptionCheck(userId, channelId)

        assertTrue(result!!)
    }

    @Test
    fun `should handle empty user id`() {
        val userId = ""
        val channelId = "test-channel"

        cache.cacheSubscriptionCheck(userId, channelId, true)
        val result = cache.getSubscriptionCheck(userId, channelId)

        assertTrue(result!!)
    }

    @Test
    fun `should handle empty channel list`() {
        val userId = "user123"
        val channels = emptyList<String>()

        cache.cacheSubscribableChannels(userId, channels)
        val result = cache.getSubscribableChannels(userId)

        assertEquals(channels, result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun `should clear user cache entries`() {
        val userId = "user123"

        // Cache multiple types of entries for the user
        cache.cacheSubscriptionCheck(userId, "channel1", true)
        cache.cacheSubscribableChannels(userId, listOf("channel1", "channel2"))
        cache.cacheSubscribableChannelsByPattern(userId, "news.*", listOf("news.sports"))

        // Verify they exist
        assertTrue(cache.getSubscriptionCheck(userId, "channel1")!!)
        assertEquals(2, cache.getSubscribableChannels(userId)!!.size)
        assertEquals(1, cache.getSubscribableChannelsByPattern(userId, "news.*")!!.size)

        // Clear user cache
        cache.clearUserCache(userId)

        // Verify they're gone
        assertNull(cache.getSubscriptionCheck(userId, "channel1"))
        assertNull(cache.getSubscribableChannels(userId))
        assertNull(cache.getSubscribableChannelsByPattern(userId, "news.*"))
    }
}
