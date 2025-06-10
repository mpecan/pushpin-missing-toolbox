package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.security.oauth2.jwt.Jwt

class NoOpChannelSubscriptionExtractorServiceTest {
    private val service = NoOpChannelSubscriptionExtractorService()

    @Test
    fun `should return null when extracting channel subscriptions`() {
        val jwt: Jwt = mock()

        val result = service.extractChannelSubscriptions(jwt)

        assertNull(result)
    }

    @Test
    fun `should return default channels claim path`() {
        val result = service.getChannelsClaimPath()

        assertEquals("$.channels", result)
    }

    @Test
    fun `should return false for claim extraction enabled`() {
        val result = service.isClaimExtractionEnabled()

        assertFalse(result)
    }
}
