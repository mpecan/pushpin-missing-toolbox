package io.github.mpecan.pmt.security.service

import io.github.mpecan.pmt.security.core.ChannelSubscriptionService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing channel subscription authorization.
 * This service determines which channels users can subscribe to for receiving
 * real-time updates through Pushpin.
 */
@Service
class ChannelSubscriptionAuthorizationService : ChannelSubscriptionService {
    
    // Map of username to allowed channel patterns
    private val userChannelPatterns = ConcurrentHashMap<String, MutableSet<String>>()
    
    // Map of role to allowed channel patterns
    private val roleChannelPatterns = ConcurrentHashMap<String, MutableSet<String>>()
    
    /**
     * Grant subscription access to a user for specific channel patterns.
     */
    fun grantUserAccess(username: String, vararg channelPatterns: String) {
        val patterns = userChannelPatterns.getOrPut(username) { mutableSetOf() }
        patterns.addAll(channelPatterns)
    }
    
    /**
     * Grant subscription access to a role for specific channel patterns.
     */
    fun grantRoleAccess(role: String, vararg channelPatterns: String) {
        val patterns = roleChannelPatterns.getOrPut(role) { mutableSetOf() }
        patterns.addAll(channelPatterns)
    }
    
    /**
     * Revoke subscription access from a user for specific channel patterns.
     */
    fun revokeUserAccess(username: String, vararg channelPatterns: String) {
        userChannelPatterns[username]?.removeAll(channelPatterns.toSet())
    }
    
    /**
     * Revoke subscription access from a role for specific channel patterns.
     */
    fun revokeRoleAccess(role: String, vararg channelPatterns: String) {
        roleChannelPatterns[role]?.removeAll(channelPatterns.toSet())
    }
    
    /**
     * Check if a principal can subscribe to a specific channel.
     * The principal can be a username (String) or an Authentication object.
     */
    override fun canSubscribe(principal: Any?, channelId: String): Boolean {
        if (principal == null) return false
        
        return when (principal) {
            is String -> canUserSubscribe(principal, channelId)
            is Authentication -> canAuthenticationSubscribe(principal, channelId)
            else -> false
        }
    }
    
    /**
     * Get all channels that the principal can subscribe to.
     */
    override fun getSubscribableChannels(principal: Any?): List<String> {
        if (principal == null) return emptyList()
        
        return when (principal) {
            is String -> getUserSubscribablePatterns(principal)
            is Authentication -> getAuthenticationSubscribablePatterns(principal)
            else -> emptyList()
        }
    }
    
    /**
     * Get channels matching a pattern that the principal can subscribe to.
     */
    override fun getSubscribableChannelsByPattern(principal: Any?, pattern: String): List<String> {
        // In a real implementation, this would query actual channels matching the pattern
        // For now, we just return the patterns the user has access to that match
        return getSubscribableChannels(principal).filter { it.matches(pattern.toRegex()) }
    }
    
    private fun canUserSubscribe(username: String, channelId: String): Boolean {
        val patterns = userChannelPatterns[username] ?: emptySet()
        return patterns.any { pattern -> matchesPattern(channelId, pattern) }
    }
    
    private fun canAuthenticationSubscribe(auth: Authentication, channelId: String): Boolean {
        // Check user-specific patterns
        if (canUserSubscribe(auth.name, channelId)) {
            return true
        }
        
        // Check role-based patterns
        return auth.authorities.any { authority ->
            val patterns = roleChannelPatterns[authority.authority] ?: emptySet()
            patterns.any { pattern -> matchesPattern(channelId, pattern) }
        }
    }
    
    private fun getUserSubscribablePatterns(username: String): List<String> {
        return userChannelPatterns[username]?.toList() ?: emptyList()
    }
    
    private fun getAuthenticationSubscribablePatterns(auth: Authentication): List<String> {
        val userPatterns = getUserSubscribablePatterns(auth.name)
        val rolePatterns = auth.authorities.flatMap { authority ->
            roleChannelPatterns[authority.authority]?.toList() ?: emptyList()
        }
        return (userPatterns + rolePatterns).distinct()
    }
    
    private fun matchesPattern(channelId: String, pattern: String): Boolean {
        // Simple pattern matching - supports exact match and wildcard suffix
        return when {
            pattern == channelId -> true
            pattern.endsWith("*") -> channelId.startsWith(pattern.dropLast(1))
            else -> false
        }
    }
}