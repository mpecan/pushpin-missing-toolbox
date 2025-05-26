package io.github.mpecan.pmt.security.core

import io.github.mpecan.pmt.security.model.ChannelSubscriptions
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Service for extracting channel subscriptions from JWT tokens.
 */
interface ChannelSubscriptionExtractorService {
    /**
     * Extract channel subscriptions from the JWT token.
     *
     * The expected format in the JWT can be either:
     * 1. An array of channel IDs (user can subscribe to all listed channels):
     *    { "channels": ["channel1", "channel2", "news.*"] }
     * 
     * 2. An array of channel objects with metadata:
     *    { "channels": [{ "id": "channel1", "expires": "2024-12-31" }] }
     *
     * 3. A map of channel IDs to metadata:
     *    { "channels": { "channel1": { "expires": "2024-12-31" }, "channel2": {} } }
     *
     * @param jwt The JWT token
     * @return ChannelSubscriptions object or null if no channels found
     */
    fun extractChannelSubscriptions(jwt: Jwt): ChannelSubscriptions?
    
    /**
     * Get the configured path to the channels claim in the JWT token.
     */
    fun getChannelsClaimPath(): String
    
    /**
     * Check if claim extraction is enabled.
     */
    fun isClaimExtractionEnabled(): Boolean
}