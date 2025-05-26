package io.github.mpecan.pmt.service

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.encryption.ChannelEncryptionService
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.service.EnhancedChannelAuthorizationService
import io.github.mpecan.pmt.service.zmq.ZmqPublisher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Enhanced service for managing Pushpin servers and publishing messages.
 * Extends the original PushpinService with support for:
 * - JWT-based channel permissions
 * - Remote authorization API integration
 */
@Service
class EnhancedPushpinService(
    private val pushpinService: PushpinService,
    private val pushpinProperties: PushpinProperties,
    private val enhancedChannelAuthorizationService: EnhancedChannelAuthorizationService,
    private val auditLogService: AuditLogService
) {
    private val logger = LoggerFactory.getLogger(EnhancedPushpinService::class.java)

    /**
     * Publishes a message to Pushpin servers with enhanced authorization checks.
     * This method first checks if the user has permission to publish to the channel
     * using the enhanced authorization service, which supports:
     * - JWT token claims
     * - Remote authorization API
     * - Local permissions
     *
     * @param message The message to publish
     * @return A Mono<Boolean> indicating success or failure
     */
    fun publishMessage(message: Message): Mono<Boolean> {
        // Check if user has permission to publish to this channel
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && pushpinProperties.authEnabled) {
            // Get the current HTTP request if available
            val request = getCurrentHttpRequest()
            
            // Check channel authorization with enhanced service
            if (!hasPermissionToPublish(authentication, message.channel, request)) {
                val username = authentication.name
                auditLogService.logAuthorizationFailure(
                    username,
                    request?.remoteAddr ?: "unknown",
                    "channel:${message.channel}",
                    "WRITE"
                )
                return Mono.error(AccessDeniedException("No permission to publish to channel ${message.channel}"))
            }

            // Log channel access
            auditLogService.logChannelAccess(
                authentication.name,
                request?.remoteAddr ?: "unknown",
                message.channel,
                "publish message"
            )
        }
        
        // Delegate to the original service for actual publishing
        return pushpinService.publishMessage(message)
    }

    /**
     * Checks if the user has permission to publish to the specified channel.
     * This uses the enhanced authorization service which checks multiple sources:
     * - JWT token claims
     * - Remote authorization API
     * - Local permissions
     *
     * @param authentication The authentication object
     * @param channelId The channel ID to check permissions for
     * @param request Optional HTTP request for remote authorization
     * @return True if the user has permission to publish, false otherwise
     */
    private fun hasPermissionToPublish(
        authentication: Authentication, 
        channelId: String,
        request: HttpServletRequest? = null
    ): Boolean {
        return enhancedChannelAuthorizationService.hasPermission(
            authentication, 
            channelId, 
            ChannelPermission.WRITE,
            request
        )
    }
    
    /**
     * Get the current HTTP request from the request context.
     *
     * @return The current HTTP request, or null if not available
     */
    private fun getCurrentHttpRequest(): HttpServletRequest? {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            requestAttributes.request
        } else {
            null
        }
    }
}