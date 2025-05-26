package io.github.mpecan.pmt.security.core

import io.github.mpecan.pmt.security.model.ChannelSubscriptions
import org.springframework.security.oauth2.jwt.Jwt

/**
 * No-op implementation of ChannelSubscriptionExtractorService that is used when JWT functionality is not available.
 */
class NoOpChannelSubscriptionExtractorService : ChannelSubscriptionExtractorService {
    override fun extractChannelSubscriptions(jwt: Jwt): ChannelSubscriptions? = null
    
    override fun getChannelsClaimPath(): String = "$.channels"
    
    override fun isClaimExtractionEnabled(): Boolean = false
}