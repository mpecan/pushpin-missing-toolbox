package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Implementation of RemoteAuthorizationClient that uses HTTP to communicate with a remote
 * authorization service.
 */
@Component
@ConditionalOnProperty(name = ["pushpin.security.jwt.remoteAuthorization.enabled"], havingValue = "true")
class HttpRemoteAuthorizationClient(
    private val properties: PushpinProperties,
    private val cache: AuthorizationCache,
    private val auditLogService: AuditLogService,
    private val restTemplate: RestTemplate
) : RemoteAuthorizationClient {
    private val logger = LoggerFactory.getLogger(HttpRemoteAuthorizationClient::class.java)

    override fun hasPermission(
        request: HttpServletRequest,
        channelId: String,
        permission: ChannelPermission
    ): Boolean {
        val userId = getCurrentUserId() ?: return false
        
        // Check cache first if enabled
        if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
            cache.getPermissionCheck(userId, channelId, permission)?.let {
                logger.debug("Cache hit for permission check: user={}, channel={}, permission={}, result={}", 
                    userId, channelId, permission, it)
                return it
            }
        }
        
        try {
            val result = when (properties.security.jwt.remoteAuthorization.method.uppercase()) {
                "GET" -> checkPermissionWithGet(request, userId, channelId, permission)
                else -> checkPermissionWithPost(request, userId, channelId, permission)
            }
            
            // Cache the result if enabled
            if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
                cache.cachePermissionCheck(userId, channelId, permission, result)
            }
            
            // Audit log the result
            if (!result) {
                auditLogService.logAuthorizationFailure(userId, "remote-request", channelId, permission.name)
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Error checking permission with remote service: {}", e.message)
            auditLogService.logAuthorizationFailure(userId, "remote-request", channelId, 
                "ERROR: ${e.message ?: "Unknown error"}")
            return false
        }
    }

    override fun getChannelsWithPermission(
        request: HttpServletRequest,
        permission: ChannelPermission
    ): List<String> {
        val userId = getCurrentUserId() ?: return emptyList()
        
        // Check cache first if enabled
        if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
            cache.getChannelsWithPermission(userId, permission)?.let {
                logger.debug("Cache hit for channels with permission: user={}, permission={}, channels={}", 
                    userId, permission, it)
                return it
            }
        }
        
        try {
            val result = when (properties.security.jwt.remoteAuthorization.method.uppercase()) {
                "GET" -> getChannelsWithPermissionWithGet(request, userId, permission)
                else -> getChannelsWithPermissionWithPost(request, userId, permission)
            }
            
            // Cache the result if enabled
            if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
                cache.cacheChannelsWithPermission(userId, permission, result)
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Error getting channels with permission from remote service: {}", e.message)
            return emptyList()
        }
    }

    override fun getAllChannelPermissions(request: HttpServletRequest): List<ChannelPermissions> {
        val userId = getCurrentUserId() ?: return emptyList()
        
        // Check cache first if enabled
        if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
            cache.getAllChannelPermissions(userId)?.let {
                logger.debug("Cache hit for all channel permissions: user={}, permissions={}", userId, it)
                return it
            }
        }
        
        try {
            val result = when (properties.security.jwt.remoteAuthorization.method.uppercase()) {
                "GET" -> getAllChannelPermissionsWithGet(request, userId)
                else -> getAllChannelPermissionsWithPost(request, userId)
            }
            
            // Cache the result if enabled
            if (properties.security.jwt.remoteAuthorization.cacheEnabled) {
                cache.cacheAllChannelPermissions(userId, result)
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Error getting all channel permissions from remote service: {}", e.message)
            return emptyList()
        }
    }
    
    /**
     * Check permission using HTTP GET.
     */
    private fun checkPermissionWithGet(
        request: HttpServletRequest,
        userId: String,
        channelId: String,
        permission: ChannelPermission
    ): Boolean {
        val uri = UriComponentsBuilder.fromUriString(properties.security.jwt.remoteAuthorization.url)
            .path("/check")
            .queryParam("userId", userId)
            .queryParam("channelId", channelId)
            .queryParam("permission", permission.name)
            .build()
            .toUri()
            
        val headers = createHeaders(request)
        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            AuthorizationResponse::class.java
        )
        
        return response.body?.allowed ?: false
    }
    
    /**
     * Check permission using HTTP POST.
     */
    private fun checkPermissionWithPost(
        request: HttpServletRequest,
        userId: String,
        channelId: String,
        permission: ChannelPermission
    ): Boolean {
        val uri = URI.create("${properties.security.jwt.remoteAuthorization.url}/check")
        
        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON
        
        val body = mapOf(
            "userId" to userId,
            "channelId" to channelId,
            "permission" to permission.name
        )
        
        val response = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            HttpEntity(body, headers),
            AuthorizationResponse::class.java
        )
        
        return response.body?.allowed ?: false
    }
    
    /**
     * Get channels with permission using HTTP GET.
     */
    private fun getChannelsWithPermissionWithGet(
        request: HttpServletRequest,
        userId: String,
        permission: ChannelPermission
    ): List<String> {
        val uri = UriComponentsBuilder.fromUriString(properties.security.jwt.remoteAuthorization.url)
            .path("/channels")
            .queryParam("userId", userId)
            .queryParam("permission", permission.name)
            .build()
            .toUri()
            
        val headers = createHeaders(request)
        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            ChannelsResponse::class.java
        )
        
        return response.body?.channels ?: emptyList()
    }
    
    /**
     * Get channels with permission using HTTP POST.
     */
    private fun getChannelsWithPermissionWithPost(
        request: HttpServletRequest,
        userId: String,
        permission: ChannelPermission
    ): List<String> {
        val uri = URI.create("${properties.security.jwt.remoteAuthorization.url}/channels")
        
        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON
        
        val body = mapOf(
            "userId" to userId,
            "permission" to permission.name
        )
        
        val response = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            HttpEntity(body, headers),
            ChannelsResponse::class.java
        )
        
        return response.body?.channels ?: emptyList()
    }
    
    /**
     * Get all channel permissions using HTTP GET.
     */
    private fun getAllChannelPermissionsWithGet(
        request: HttpServletRequest,
        userId: String
    ): List<ChannelPermissions> {
        val uri = UriComponentsBuilder.fromUriString(properties.security.jwt.remoteAuthorization.url)
            .path("/permissions")
            .queryParam("userId", userId)
            .build()
            .toUri()
            
        val headers = createHeaders(request)
        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            PermissionsResponse::class.java
        )
        
        return response.body?.permissions?.map { (channelId, permissions) ->
            ChannelPermissions(
                channelId,
                permissions.mapNotNull {
                    try {
                        ChannelPermission.valueOf(it.uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }.toSet()
            )
        } ?: emptyList()
    }
    
    /**
     * Get all channel permissions using HTTP POST.
     */
    private fun getAllChannelPermissionsWithPost(
        request: HttpServletRequest,
        userId: String
    ): List<ChannelPermissions> {
        val uri = URI.create("${properties.security.jwt.remoteAuthorization.url}/permissions")
        
        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON
        
        val body = mapOf(
            "userId" to userId
        )
        
        val response = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            HttpEntity(body, headers),
            PermissionsResponse::class.java
        )
        
        return response.body?.permissions?.map { (channelId, permissions) ->
            ChannelPermissions(
                channelId,
                permissions.mapNotNull {
                    try {
                        ChannelPermission.valueOf(it.uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }.toSet()
            )
        } ?: emptyList()
    }
    
    /**
     * Create HTTP headers for the request.
     */
    private fun createHeaders(request: HttpServletRequest): HttpHeaders {
        val headers = HttpHeaders()
        
        // Include configured headers from the original request
        for (headerName in properties.security.jwt.remoteAuthorization.includeHeaders) {
            val headerValue = request.getHeader(headerName)
            if (headerValue != null) {
                headers.add(headerName, headerValue)
            }
        }
        
        return headers
    }
    
    /**
     * Get the current user ID from the security context.
     */
    private fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated && 
                  authentication.name != "anonymousUser") {
            authentication.name
        } else {
            null
        }
    }
    
    /**
     * Response from the authorization service for permission checks.
     */
    internal data class AuthorizationResponse(val allowed: Boolean)
    
    /**
     * Response from the authorization service for channel lists.
     */
    internal data class ChannelsResponse(val channels: List<String>)
    
    /**
     * Response from the authorization service for all permissions.
     */
    internal data class PermissionsResponse(val permissions: Map<String, List<String>>)
}