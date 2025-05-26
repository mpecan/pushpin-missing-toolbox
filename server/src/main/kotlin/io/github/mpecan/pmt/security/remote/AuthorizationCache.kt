package io.github.mpecan.pmt.security.remote

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Cache for authorization decisions to reduce load on remote authorization service.
 */
@Component
class AuthorizationCache(properties: PushpinProperties) {
    
    private val cache: Cache<CacheKey, CacheValue> = Caffeine.newBuilder()
        .maximumSize(properties.security.jwt.remoteAuthorization.cacheMaxSize)
        .expireAfterWrite(properties.security.jwt.remoteAuthorization.cacheTtl, TimeUnit.MILLISECONDS)
        .build()
    
    /**
     * Check if a permission check is cached.
     *
     * @param userId User ID to check
     * @param channelId Channel ID to check
     * @param permission Permission to check
     * @return True if the user has the specified permission for the channel, false otherwise, or null if not cached
     */
    fun getPermissionCheck(userId: String, channelId: String, permission: ChannelPermission): Boolean? {
        val key = CacheKey.forPermissionCheck(userId, channelId, permission)
        val value = cache.getIfPresent(key)
        return value?.boolValue
    }
    
    /**
     * Cache a permission check result.
     *
     * @param userId User ID
     * @param channelId Channel ID
     * @param permission Permission
     * @param hasPermission Result of the permission check
     */
    fun cachePermissionCheck(userId: String, channelId: String, permission: ChannelPermission, hasPermission: Boolean) {
        val key = CacheKey.forPermissionCheck(userId, channelId, permission)
        cache.put(key, CacheValue.ofBoolean(hasPermission))
    }
    
    /**
     * Get cached channels with a specific permission.
     *
     * @param userId User ID
     * @param permission Permission to check
     * @return List of channel IDs, or null if not cached
     */
    fun getChannelsWithPermission(userId: String, permission: ChannelPermission): List<String>? {
        val key = CacheKey.forChannelsList(userId, permission)
        val value = cache.getIfPresent(key)
        return value?.listValue
    }
    
    /**
     * Cache channels with a specific permission.
     *
     * @param userId User ID
     * @param permission Permission
     * @param channels List of channel IDs
     */
    fun cacheChannelsWithPermission(userId: String, permission: ChannelPermission, channels: List<String>) {
        val key = CacheKey.forChannelsList(userId, permission)
        cache.put(key, CacheValue.ofList(channels))
    }
    
    /**
     * Get all cached channel permissions for a user.
     *
     * @param userId User ID
     * @return List of ChannelPermissions, or null if not cached
     */
    fun getAllChannelPermissions(userId: String): List<ChannelPermissions>? {
        val key = CacheKey.forAllPermissions(userId)
        val value = cache.getIfPresent(key)
        return value?.permissionsValue
    }
    
    /**
     * Cache all channel permissions for a user.
     *
     * @param userId User ID
     * @param permissions List of ChannelPermissions
     */
    fun cacheAllChannelPermissions(userId: String, permissions: List<ChannelPermissions>) {
        val key = CacheKey.forAllPermissions(userId)
        cache.put(key, CacheValue.ofPermissions(permissions))
    }
    
    /**
     * Key for cache entries.
     */
    private data class CacheKey(
        val type: CacheType,
        val userId: String,
        val channelId: String? = null,
        val permission: ChannelPermission? = null
    ) {
        companion object {
            fun forPermissionCheck(userId: String, channelId: String, permission: ChannelPermission): CacheKey {
                return CacheKey(CacheType.PERMISSION_CHECK, userId, channelId, permission)
            }
            
            fun forChannelsList(userId: String, permission: ChannelPermission): CacheKey {
                return CacheKey(CacheType.CHANNELS_LIST, userId, permission = permission)
            }
            
            fun forAllPermissions(userId: String): CacheKey {
                return CacheKey(CacheType.ALL_PERMISSIONS, userId)
            }
        }
    }
    
    /**
     * Type of cache entry.
     */
    private enum class CacheType {
        PERMISSION_CHECK,
        CHANNELS_LIST,
        ALL_PERMISSIONS
    }
    
    /**
     * Value for cache entries.
     */
    private data class CacheValue(
        val boolValue: Boolean? = null,
        val listValue: List<String>? = null,
        val permissionsValue: List<ChannelPermissions>? = null
    ) {
        companion object {
            fun ofBoolean(value: Boolean): CacheValue {
                return CacheValue(boolValue = value)
            }
            
            fun ofList(value: List<String>): CacheValue {
                return CacheValue(listValue = value)
            }
            
            fun ofPermissions(value: List<ChannelPermissions>): CacheValue {
                return CacheValue(permissionsValue = value)
            }
        }
    }
}