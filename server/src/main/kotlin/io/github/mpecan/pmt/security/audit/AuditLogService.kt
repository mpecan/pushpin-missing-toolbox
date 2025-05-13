package io.github.mpecan.pmt.security.audit

import io.github.mpecan.pmt.config.PushpinProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for logging security-related audit events.
 */
@Service
class AuditLogService(private val properties: PushpinProperties) {
    
    private val logger = LoggerFactory.getLogger("audit")
    
    /**
     * Log an audit event.
     */
    fun log(event: AuditEvent) {
        if (!properties.security.auditLogging.enabled) {
            return
        }
        
        when (properties.security.auditLogging.level.uppercase()) {
            "DEBUG" -> logger.debug(formatEvent(event))
            "WARN" -> logger.warn(formatEvent(event))
            "ERROR" -> logger.error(formatEvent(event))
            else -> logger.info(formatEvent(event))
        }
    }
    
    /**
     * Format an audit event as a string.
     */
    private fun formatEvent(event: AuditEvent): String {
        return "AUDIT: ${event.type} | User: ${event.username} | ${event.details} | IP: ${event.ipAddress} | Time: ${event.timestamp}"
    }
    
    /**
     * Log an authentication success event.
     */
    fun logAuthSuccess(username: String, ipAddress: String, details: String = "") {
        log(AuditEvent(
            type = AuditEventType.AUTHENTICATION_SUCCESS,
            username = username,
            ipAddress = ipAddress,
            details = details
        ))
    }
    
    /**
     * Log an authentication failure event.
     */
    fun logAuthFailure(username: String, ipAddress: String, details: String = "") {
        log(AuditEvent(
            type = AuditEventType.AUTHENTICATION_FAILURE,
            username = username,
            ipAddress = ipAddress,
            details = details
        ))
    }
    
    /**
     * Log an authorization failure event.
     */
    fun logAuthorizationFailure(username: String, ipAddress: String, resource: String, permission: String) {
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
    fun logChannelAccess(username: String, ipAddress: String, channelId: String, action: String) {
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
    fun logRateLimitExceeded(username: String?, ipAddress: String) {
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
    fun logSecurityConfigChange(username: String, ipAddress: String, details: String) {
        log(AuditEvent(
            type = AuditEventType.SECURITY_CONFIG_CHANGE,
            username = username,
            ipAddress = ipAddress,
            details = details
        ))
    }
}

/**
 * Represents a security audit event.
 */
data class AuditEvent(
    val type: AuditEventType,
    val username: String?,
    val ipAddress: String,
    val details: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of security audit events.
 */
enum class AuditEventType {
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE,
    CHANNEL_ACCESS,
    RATE_LIMIT_EXCEEDED,
    SECURITY_CONFIG_CHANGE
}