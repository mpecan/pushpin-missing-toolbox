package io.github.mpecan.pmt.security.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ChannelSubscriptionsTest {
    @Test
    fun `should check if subscription is allowed`() {
        val subscriptions =
            ChannelSubscriptions(
                principal = "user123",
                subscriptions =
                    listOf(
                        ChannelSubscription("news", allowed = true),
                        ChannelSubscription("admin", allowed = false),
                        ChannelSubscription("user.123", allowed = true),
                    ),
                defaultAllow = false,
            )

        Assertions.assertThat(subscriptions.canSubscribe("news")).isTrue()
        Assertions.assertThat(subscriptions.canSubscribe("admin")).isFalse()
        Assertions.assertThat(subscriptions.canSubscribe("user.123")).isTrue()
        Assertions.assertThat(subscriptions.canSubscribe("unknown")).isFalse()
    }

    @Test
    fun `should respect default allow policy`() {
        val subscriptions =
            ChannelSubscriptions(
                principal = "user123",
                subscriptions =
                    listOf(
                        ChannelSubscription("blocked", allowed = false),
                    ),
                defaultAllow = true,
            )

        Assertions.assertThat(subscriptions.canSubscribe("blocked")).isFalse()
        Assertions.assertThat(subscriptions.canSubscribe("any-other-channel")).isTrue()
    }

    @Test
    fun `should get all allowed channels`() {
        val subscriptions =
            ChannelSubscriptions(
                principal = "user123",
                subscriptions =
                    listOf(
                        ChannelSubscription("news", allowed = true),
                        ChannelSubscription("admin", allowed = false),
                        ChannelSubscription("user.123", allowed = true),
                        ChannelSubscription("private", allowed = false),
                    ),
            )

        Assertions
            .assertThat(subscriptions.getAllowedChannels())
            .containsExactlyInAnyOrder("news", "user.123")
    }

    @Test
    fun `should handle empty subscriptions`() {
        val subscriptions =
            ChannelSubscriptions(
                principal = "user123",
                subscriptions = emptyList(),
                defaultAllow = false,
            )

        Assertions.assertThat(subscriptions.getAllowedChannels()).isEmpty()
        Assertions.assertThat(subscriptions.canSubscribe("any-channel")).isFalse()
    }
}
