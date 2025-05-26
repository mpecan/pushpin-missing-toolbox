package io.github.mpecan.pmt.security.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChannelSubscriptionTest {
    
    @Test
    fun `should create channel subscription with defaults`() {
        val subscription = ChannelSubscription(channelId = "news")
        
        assertThat(subscription.channelId).isEqualTo("news")
        assertThat(subscription.allowed).isTrue()
        assertThat(subscription.metadata).isEmpty()
    }
    
    @Test
    fun `should create channel subscription with metadata`() {
        val subscription = ChannelSubscription(
            channelId = "user.123",
            allowed = true,
            metadata = mapOf(
                "expiresAt" to "2024-12-31",
                "filter" to "important"
            )
        )
        
        assertThat(subscription.getMetadata<String>("expiresAt")).isEqualTo("2024-12-31")
        assertThat(subscription.getMetadata<String>("filter")).isEqualTo("important")
        assertThat(subscription.getMetadata<String>("nonexistent")).isNull()
    }
    
    @Test
    fun `should handle type-safe metadata retrieval`() {
        val subscription = ChannelSubscription(
            channelId = "analytics",
            metadata = mapOf(
                "limit" to 100,
                "active" to true,
                "tags" to listOf("sales", "revenue")
            )
        )
        
        assertThat(subscription.getMetadata<Int>("limit")).isEqualTo(100)
        assertThat(subscription.getMetadata<Boolean>("active")).isTrue()
        assertThat(subscription.getMetadata<List<String>>("tags"))
            .containsExactly("sales", "revenue")
    }
}

class ChannelSubscriptionsTest {
    
    @Test
    fun `should check if subscription is allowed`() {
        val subscriptions = ChannelSubscriptions(
            principal = "user123",
            subscriptions = listOf(
                ChannelSubscription("news", allowed = true),
                ChannelSubscription("admin", allowed = false),
                ChannelSubscription("user.123", allowed = true)
            ),
            defaultAllow = false
        )
        
        assertThat(subscriptions.canSubscribe("news")).isTrue()
        assertThat(subscriptions.canSubscribe("admin")).isFalse()
        assertThat(subscriptions.canSubscribe("user.123")).isTrue()
        assertThat(subscriptions.canSubscribe("unknown")).isFalse()
    }
    
    @Test
    fun `should respect default allow policy`() {
        val subscriptions = ChannelSubscriptions(
            principal = "user123",
            subscriptions = listOf(
                ChannelSubscription("blocked", allowed = false)
            ),
            defaultAllow = true
        )
        
        assertThat(subscriptions.canSubscribe("blocked")).isFalse()
        assertThat(subscriptions.canSubscribe("any-other-channel")).isTrue()
    }
    
    @Test
    fun `should get all allowed channels`() {
        val subscriptions = ChannelSubscriptions(
            principal = "user123",
            subscriptions = listOf(
                ChannelSubscription("news", allowed = true),
                ChannelSubscription("admin", allowed = false),
                ChannelSubscription("user.123", allowed = true),
                ChannelSubscription("private", allowed = false)
            )
        )
        
        assertThat(subscriptions.getAllowedChannels())
            .containsExactlyInAnyOrder("news", "user.123")
    }
    
    @Test
    fun `should handle empty subscriptions`() {
        val subscriptions = ChannelSubscriptions(
            principal = "user123",
            subscriptions = emptyList(),
            defaultAllow = false
        )
        
        assertThat(subscriptions.getAllowedChannels()).isEmpty()
        assertThat(subscriptions.canSubscribe("any-channel")).isFalse()
    }
}