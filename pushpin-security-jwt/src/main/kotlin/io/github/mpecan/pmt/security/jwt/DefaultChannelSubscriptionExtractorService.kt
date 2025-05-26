package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.security.core.ChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.core.ClaimExtractorService
import io.github.mpecan.pmt.security.model.ChannelSubscription
import io.github.mpecan.pmt.security.model.ChannelSubscriptions
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Default implementation for extracting channel subscriptions from JWT tokens.
 */
class DefaultChannelSubscriptionExtractorService(
    private val claimExtractorService: ClaimExtractorService,
    private val properties: JwtProperties
) : ChannelSubscriptionExtractorService {
    private val logger = LoggerFactory.getLogger(DefaultChannelSubscriptionExtractorService::class.java)

    override fun extractChannelSubscriptions(jwt: Jwt): ChannelSubscriptions? {
        if (!properties.claimExtraction.enabled) {
            return null
        }

        val principal = jwt.subject ?: "anonymous"
        
        try {
            // First try simple array format: ["channel1", "channel2"]
            val channelsList = claimExtractorService.extractListClaim(jwt, getChannelsClaimPath())
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
            val channelsMap = claimExtractorService.extractMapClaim(jwt, getChannelsClaimPath())
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
    
    override fun getChannelsClaimPath(): String {
        return properties.claimExtraction.extractClaims.find { 
            it.contains("channel") || it.contains("subscription") 
        } ?: "$.channels"
    }
    
    override fun isClaimExtractionEnabled(): Boolean = properties.claimExtraction.enabled
    
    /**
     * Try to parse channel objects in the format: [{ "id": "channel1", "expires": "2024-12-31" }]
     */
    private fun tryParseChannelObjects(jwt: Jwt): List<ChannelSubscription>? {
        try {
            val results = mutableListOf<ChannelSubscription>()
            var index = 0
            
            while (true) {
                val channelObject = claimExtractorService.extractMapClaim(jwt, "${getChannelsClaimPath()}[$index]")
                if (channelObject.isEmpty()) break
                
                val channelId = channelObject["id"] as? String 
                    ?: channelObject["channelId"] as? String
                    ?: channelObject["channel"] as? String
                    ?: continue
                
                // Extract metadata (everything except the channel ID)
                val metadata = channelObject
                    .filterKeys { it !in setOf("id", "channelId", "channel") }
                    .mapValues { it.value.toString() }
                
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