package io.github.mpecan.pmt.security.jwt

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Extracts channel permissions from JWT tokens using the configured extraction paths.
 */
@Component
class JwtChannelPermissionsExtractor(
    private val claimExtractor: ClaimExtractor,
    private val pushpinProperties: PushpinProperties
) {
    private val logger = LoggerFactory.getLogger(JwtChannelPermissionsExtractor::class.java)

    /**
     * Path to the channels claim in the JWT token.
     *
     * Format can be in the following forms depending on JWT structure:
     * - directPath
     * - $.path.to.channels (JsonPath syntax)
     */
    val channelsClaimPath: String
        get() = pushpinProperties.security.jwt.claimExtraction.extractClaims.find { 
            it.contains("channel") || it.contains("permissions") 
        } ?: "$.channels"

    /**
     * Extract channels and their permissions from the JWT token.
     *
     * The expected format in the JWT can be either:
     * 1. A map with channel IDs as keys and permission arrays as values, e.g.:
     *    { "channels": { "channel1": ["READ", "WRITE"], "channel2": ["READ"] } }
     * 
     * 2. An array of channel objects with channel ID and permissions, e.g.:
     *    { "channels": [{ "id": "channel1", "permissions": ["READ", "WRITE"] }] }
     *
     * 3. A flat array of channel:permission strings, e.g.:
     *    { "channels": ["channel1:READ", "channel1:WRITE", "channel2:READ"] }
     *
     * @param jwt The JWT token
     * @return List of channel permissions
     */
    fun extractChannelPermissions(jwt: Jwt): List<ChannelPermissions> {
        if (!pushpinProperties.security.jwt.claimExtraction.enabled) {
            return emptyList()
        }

        try {
            // First try map format: { "channels": { "channel1": ["READ", "WRITE"] } }
            val channelsMap = claimExtractor.extractMapClaim(jwt, channelsClaimPath)
            
            if (channelsMap.isNotEmpty()) {
                return channelsMap.map { (channelId, permissions) ->
                    val permSet = when (permissions) {
                        is List<*> -> permissions.mapNotNull { parsePermission(it.toString()) }.toSet()
                        is String -> permissions.split(",").mapNotNull { parsePermission(it.trim()) }.toSet()
                        else -> permissions.toString().split(",").mapNotNull { parsePermission(it.trim()) }.toSet()
                    }
                    ChannelPermissions(channelId, permSet)
                }.filter { it.permissions.isNotEmpty() }
            }
            
            // Try array of objects: [{ "id": "channel1", "permissions": ["READ", "WRITE"] }]
            val channelsList = claimExtractor.extractListClaim(jwt, channelsClaimPath)
            
            if (channelsList.isNotEmpty()) {
                return tryParseChannelObjects(jwt, channelsClaimPath) ?: 
                       tryParseChannelStrings(channelsList)
            }
        } catch (e: Exception) {
            logger.warn("Error extracting channel permissions from JWT: {}", e.message)
        }
        
        return emptyList()
    }
    
    /**
     * Try to parse channel objects in the format: [{ "id": "channel1", "permissions": ["READ", "WRITE"] }]
     */
    private fun tryParseChannelObjects(jwt: Jwt, basePath: String): List<ChannelPermissions>? {
        try {
            // Check for channel objects with 'id' and 'permissions' fields
            val objects = claimExtractor.extractMapClaim(jwt, "$basePath[0]")
            
            if (objects.containsKey("id") || objects.containsKey("channelId")) {
                val results = mutableListOf<ChannelPermissions>()
                var index = 0
                
                while (true) {
                    val channelObject = claimExtractor.extractMapClaim(jwt, "$basePath[$index]")
                    if (channelObject.isEmpty()) break
                    
                    val channelId = channelObject["id"] as? String 
                        ?: channelObject["channelId"] as? String
                        ?: continue
                        
                    val permissions = when (val perms = channelObject["permissions"]) {
                        is List<*> -> perms.mapNotNull { 
                            it?.toString()?.let { parsePermission(it) } 
                        }.toSet()
                        is String -> perms.split(",").mapNotNull { 
                            parsePermission(it.trim()) 
                        }.toSet()
                        null -> emptySet()
                        else -> parsePermission(perms.toString())?.let { setOf(it) } ?: emptySet()
                    }
                    
                    if (permissions.isNotEmpty()) {
                        results.add(ChannelPermissions(channelId, permissions))
                    }
                    
                    index++
                }
                
                return results
            }
        } catch (e: Exception) {
            logger.debug("Not in channel objects format: {}", e.message)
        }
        
        return null
    }
    
    /**
     * Try to parse flat channel strings in the format: ["channel1:READ", "channel1:WRITE"]
     */
    private fun tryParseChannelStrings(strings: List<String>): List<ChannelPermissions> {
        val channelPermissions = mutableMapOf<String, MutableSet<ChannelPermission>>()
        
        for (item in strings) {
            val parts = item.split(":", limit = 2)
            if (parts.size == 2) {
                val channelId = parts[0].trim()
                val permission = parsePermission(parts[1].trim())
                
                if (permission != null) {
                    channelPermissions
                        .computeIfAbsent(channelId) { mutableSetOf() }
                        .add(permission)
                }
            }
        }
        
        return channelPermissions.map { (channelId, permissions) ->
            ChannelPermissions(channelId, permissions)
        }
    }
    
    /**
     * Parse a permission string into a ChannelPermission.
     */
    private fun parsePermission(value: String): ChannelPermission? {
        return try {
            ChannelPermission.valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid permission value: {}", value)
            null
        }
    }
}