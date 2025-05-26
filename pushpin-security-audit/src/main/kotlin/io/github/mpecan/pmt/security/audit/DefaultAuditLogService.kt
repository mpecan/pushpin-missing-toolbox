package io.github.mpecan.pmt.security.audit

import io.github.mpecan.pmt.security.core.AuditEvent
import io.github.mpecan.pmt.security.core.AuditEventType
import io.github.mpecan.pmt.security.core.AuditService
import org.slf4j.LoggerFactory

/**
 * Default implementation of AuditService that logs to SLF4J.
 */
class DefaultAuditLogService(
    private val properties: AuditProperties
) : AuditService {
    
    private val logger = LoggerFactory.getLogger("audit")
    
    /**
     * Log an audit event.
     */
    override fun log(event: AuditEvent) {
        if (!properties.enabled) {
            return
        }
        
        // Filter events based on configuration
        when (event.type) {
            AuditEventType.AUTHENTICATION_SUCCESS -> {
                if (!properties.logSuccessfulAuth) return
            }
            AuditEventType.AUTHENTICATION_FAILURE -> {
                if (!properties.logFailedAuth) return
            }
            AuditEventType.CHANNEL_ACCESS -> {
                if (!properties.logChannelAccess) return
            }
            AuditEventType.SECURITY_CONFIG_CHANGE -> {
                if (!properties.logAdminActions) return
            }
            else -> {
                // Always log other types (authorization failures, rate limits, etc.)
            }
        }
        
        val message = formatEvent(event)
        
        when (properties.level.uppercase()) {
            "DEBUG" -> logger.debug(message)
            "WARN" -> logger.warn(message)
            "ERROR" -> logger.error(message)
            else -> logger.info(message)
        }
    }
    
    /**
     * Format an audit event as a string.
     */
    private fun formatEvent(event: AuditEvent): String {
        val builder = StringBuilder()
        builder.append("AUDIT: ${event.type}")
        
        event.username?.let { builder.append(" | User: $it") }
        builder.append(" | ${event.details}")
        builder.append(" | IP: ${event.ipAddress}")
        builder.append(" | Time: ${event.timestamp}")
        
        return builder.toString()
    }
    
    /**
     * Log an authentication success event.
     */
    override fun logAuthSuccess(username: String, ipAddress: String, details: String) {
        log(AuditEvent(
            type = AuditEventType.AUTHENTICATION_SUCCESS,
            username = username,
            ipAddress = ipAddress,
            details = details.ifEmpty { "Authentication successful" }
        ))
    }
    
    /**
     * Log an authentication failure event.
     */
    override fun logAuthFailure(username: String, ipAddress: String, details: String) {
        log(AuditEvent(
            type = AuditEventType.AUTHENTICATION_FAILURE,
            username = username,
            ipAddress = ipAddress,
            details = details.ifEmpty { "Authentication failed" }
        ))
    }
    
    /**
     * Log an authorization failure event.
     */
    override fun logAuthorizationFailure(username: String, ipAddress: String, resource: String, permission: String) {
        log(AuditEvent(
            type = AuditEventType.AUTHORIZATION_FAILURE,
            username = username,
            ipAddress = ipAddress,
            details = "Access denied to resource '$resource' with permission '$permission'"
        ))
    }
    
    /**
     * Log a channel access event.
     */
    override fun logChannelAccess(username: String, ipAddress: String, channelId: String, action: String) {
        log(AuditEvent(
            type = AuditEventType.CHANNEL_ACCESS,
            username = username,
            ipAddress = ipAddress,
            details = "Channel '$channelId' $action"
        ))
    }
    
    /**
     * Log a rate limit exceeded event.
     */
    override fun logRateLimitExceeded(username: String?, ipAddress: String) {
        log(AuditEvent(
            type = AuditEventType.RATE_LIMIT_EXCEEDED,
            username = username,
            ipAddress = ipAddress,
            details = "Rate limit exceeded"
        ))
    }
    
    /**
     * Log a security configuration change event.
     */
    override fun logSecurityConfigChange(username: String, ipAddress: String, details: String) {
        log(AuditEvent(
            type = AuditEventType.SECURITY_CONFIG_CHANGE,
            username = username,
            ipAddress = ipAddress,
            details = details
        ))
    }
}