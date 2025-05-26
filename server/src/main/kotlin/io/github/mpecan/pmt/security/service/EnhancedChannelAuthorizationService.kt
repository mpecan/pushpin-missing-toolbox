package io.github.mpecan.pmt.security.service

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.jwt.JwtChannelPermissionsExtractor
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import io.github.mpecan.pmt.security.remote.RemoteAuthorizationClient
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced service for managing channel permissions and authorization.
 * Extends the original ChannelAuthorizationService with support for:
 * - JWT claim extraction for channel permissions
 * - Remote authorization API integration
 */
@Service
class EnhancedChannelAuthorizationService(
    private val channelAuthorizationService: ChannelAuthorizationService,
    private val jwtChannelPermissionsExtractor: JwtChannelPermissionsExtractor,
    private val remoteAuthorizationClient: RemoteAuthorizationClient,
    private val pushpinProperties: PushpinProperties
) {
    private val logger = LoggerFactory.getLogger(EnhancedChannelAuthorizationService::class.java)
    
    // Cache of JWT token permissions to avoid repeated extraction
    private val jwtPermissionsCache = ConcurrentHashMap<String, List<ChannelPermissions>>()

    /**
     * Check if the authentication has the required permission for a channel.
     * This method checks multiple sources in the following order:
     * 1. JWT token claims (if JWT authentication and claim extraction is enabled)
     * 2. Remote authorization service (if enabled)
     * 3. Local permissions stored in ChannelAuthorizationService
     *
     * @param authentication The authentication object
     * @param channelId The channel ID to check permissions for
     * @param permission The required permission
     * @param request Optional HTTP request for remote authorization
     * @return True if the authentication has the required permission, false otherwise
     */
    fun hasPermission(
        authentication: Authentication,
        channelId: String,
        permission: ChannelPermission,
        request: HttpServletRequest? = null
    ): Boolean {
        // 1. Check JWT claims for permissions if enabled and authentication is JWT
        if (pushpinProperties.security.jwt.claimExtraction.enabled && 
            authentication is JwtAuthenticationToken) {
            
            val jwt = authentication.token
            val jwtPermissions = getPermissionsFromJwt(jwt)
            
            // Check if the JWT contains the required permission for the channel
            val channelPerms = jwtPermissions.find { it.channelId == channelId }
            if (channelPerms != null) {
                val hasPermission = when (permission) {
                    ChannelPermission.READ -> channelPerms.hasReadPermission()
                    ChannelPermission.WRITE -> channelPerms.hasWritePermission()
                    ChannelPermission.ADMIN -> channelPerms.hasAdminPermission()
                }
                
                if (hasPermission) {
                    logger.debug("Permission granted from JWT claims: user={}, channel={}, permission={}",
                        authentication.name, channelId, permission)
                    return true
                }
            }
        }
        
        // 2. Check remote authorization service if enabled and we have a request
        if (pushpinProperties.security.jwt.remoteAuthorization.enabled && request != null) {
            val hasPermission = remoteAuthorizationClient.hasPermission(request, channelId, permission)
            if (hasPermission) {
                logger.debug("Permission granted from remote authorization: user={}, channel={}, permission={}",
                    authentication.name, channelId, permission)
                return true
            }
        }
        
        // 3. Check local permissions
        return channelAuthorizationService.hasPermission(authentication, channelId, permission)
    }
    
    /**
     * Get all channels that an authentication has access to with the given permission.
     * This combines channels from multiple sources:
     * 1. JWT token claims (if JWT authentication and claim extraction is enabled)
     * 2. Remote authorization service (if enabled)
     * 3. Local permissions stored in ChannelAuthorizationService
     *
     * @param authentication The authentication object
     * @param permission The required permission
     * @param request Optional HTTP request for remote authorization
     * @return List of channel IDs accessible with the given permission
     */
    fun getChannelsWithPermission(
        authentication: Authentication,
        permission: ChannelPermission,
        request: HttpServletRequest? = null
    ): List<String> {
        val allChannels = mutableSetOf<String>()
        
        // 1. Add channels from JWT claims
        if (pushpinProperties.security.jwt.claimExtraction.enabled && 
            authentication is JwtAuthenticationToken) {
            
            val jwt = authentication.token
            val jwtPermissions = getPermissionsFromJwt(jwt)
            
            val jwtChannels = jwtPermissions
                .filter { channelPerms ->
                    when (permission) {
                        ChannelPermission.READ -> channelPerms.hasReadPermission()
                        ChannelPermission.WRITE -> channelPerms.hasWritePermission()
                        ChannelPermission.ADMIN -> channelPerms.hasAdminPermission()
                    }
                }
                .map { it.channelId }
            
            allChannels.addAll(jwtChannels)
        }
        
        // 2. Add channels from remote authorization service
        if (pushpinProperties.security.jwt.remoteAuthorization.enabled && request != null) {
            val remoteChannels = remoteAuthorizationClient.getChannelsWithPermission(request, permission)
            allChannels.addAll(remoteChannels)
        }
        
        // 3. Add channels from local permissions
        val localChannels = channelAuthorizationService.getChannelsWithPermission(authentication, permission)
        allChannels.addAll(localChannels)
        
        return allChannels.toList()
    }
    
    /**
     * Extract permissions from a JWT token, with caching.
     *
     * @param jwt The JWT token
     * @return List of channel permissions extracted from the token
     */
    private fun getPermissionsFromJwt(jwt: Jwt): List<ChannelPermissions> {
        // Use token ID as cache key if available, or the entire token
        val cacheKey = jwt.id ?: jwt.tokenValue
        
        // Check cache first
        return jwtPermissionsCache.computeIfAbsent(cacheKey) {
            jwtChannelPermissionsExtractor.extractChannelPermissions(jwt)
        }
    }
    
    /**
     * Clear the JWT permissions cache for testing purposes.
     */
    internal fun clearJwtPermissionsCache() {
        jwtPermissionsCache.clear()
    }
}