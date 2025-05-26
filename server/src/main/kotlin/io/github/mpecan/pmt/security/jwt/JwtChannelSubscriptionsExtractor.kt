package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.model.ChannelSubscription
import io.github.mpecan.pmt.security.model.ChannelSubscriptions
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Extracts channel subscriptions from JWT tokens using the configured extraction paths.
 */
@Component
class JwtChannelSubscriptionsExtractor(
    private val claimExtractor: ClaimExtractor,
    private val pushpinProperties: PushpinProperties
) {
    private val logger = LoggerFactory.getLogger(JwtChannelSubscriptionsExtractor::class.java)

    /**
     * Path to the channels claim in the JWT token.
     *
     * Format can be in the following forms depending on JWT structure:
     * - directPath
     * - $.path.to.channels (JsonPath syntax)
     */
    val channelsClaimPath: String
        get() = pushpinProperties.security.jwt.claimExtraction.extractClaims.find { 
            it.contains("channel") || it.contains("subscription") 
        } ?: "$.channels"

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
    fun extractChannelSubscriptions(jwt: Jwt): ChannelSubscriptions? {
        if (!pushpinProperties.security.jwt.claimExtraction.enabled) {
            return null
        }

        val principal = jwt.subject ?: "anonymous"
        
        try {
            // First try simple array format: ["channel1", "channel2"]
            val channelsList = claimExtractor.extractListClaim(jwt, channelsClaimPath)
            if (channelsList.isNotEmpty()) {
                val subscriptions = channelsList.map { channelId ->
                    ChannelSubscription(
                        channelId = channelId,
                        allowed = true
                    )
                }
                return ChannelSubscriptions(
                    principal = principal,
                    subscriptions = subscriptions,
                    defaultAllow = false
                )
            }
            
            // Try map format: { "channel1": {}, "channel2": { "expires": "2024-12-31" } }
            val channelsMap = claimExtractor.extractMapClaim(jwt, channelsClaimPath)
            if (channelsMap.isNotEmpty()) {
                val subscriptions = channelsMap.map { (channelId, metadata) ->
                    val metadataMap = when (metadata) {
                        is Map<*, *> -> metadata.mapKeys { it.key.toString() }
                            .mapValues { it.value?.toString() ?: "" }
                        else -> emptyMap()
                    }
                    ChannelSubscription(
                        channelId = channelId,
                        allowed = true,
                        metadata = metadataMap
                    )
                }
                return ChannelSubscriptions(
                    principal = principal,
                    subscriptions = subscriptions,
                    defaultAllow = false
                )
            }
            
            // Try array of objects: [{ "id": "channel1", "expires": "2024-12-31" }]
            val channelObjects = tryParseChannelObjects(jwt)
            if (channelObjects != null) {
                return ChannelSubscriptions(
                    principal = principal,
                    subscriptions = channelObjects,
                    defaultAllow = false
                )
            }
            
        } catch (e: Exception) {
            logger.warn("Error extracting channel subscriptions from JWT: {}", e.message)
        }
        
        return null
    }
    
    /**
     * Try to parse channel objects in the format: [{ "id": "channel1", "expires": "2024-12-31" }]
     */
    private fun tryParseChannelObjects(jwt: Jwt): List<ChannelSubscription>? {
        try {
            val results = mutableListOf<ChannelSubscription>()
            var index = 0
            
            while (true) {
                val channelObject = claimExtractor.extractMapClaim(jwt, "$channelsClaimPath[$index]")
                if (channelObject.isEmpty()) break
                
                val channelId = channelObject["id"] as? String 
                    ?: channelObject["channelId"] as? String
                    ?: channelObject["channel"] as? String
                    ?: continue
                
                // Extract metadata (everything except the channel ID)
                val metadata = channelObject
                    .filterKeys { it !in setOf("id", "channelId", "channel") }
                    .mapValues { it.value?.toString() ?: "" }
                
                results.add(ChannelSubscription(
                    channelId = channelId,
                    allowed = true,
                    metadata = metadata
                ))
                
                index++
            }
            
            return if (results.isNotEmpty()) results else null
        } catch (e: Exception) {
            logger.debug("Not in channel objects format: {}", e.message)
        }
        
        return null
    }
}