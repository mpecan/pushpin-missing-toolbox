package io.github.mpecan.pmt.security.core

import java.time.LocalDateTime

/**
 * Core interface for audit logging services.
 * Implementations can provide different audit strategies such as file-based,
 * database-based, or external audit systems.
 */
interface AuditService {
    /**
     * Log an audit event.
     */
    fun log(event: AuditEvent)
    
    /**
     * Log an authentication success event.
     */
    fun logAuthSuccess(username: String, ipAddress: String, details: String = "")
    
    /**
     * Log an authentication failure event.
     */
    fun logAuthFailure(username: String, ipAddress: String, details: String = "")
    
    /**
     * Log an authorization failure event.
     */
    fun logAuthorizationFailure(username: String, ipAddress: String, resource: String, permission: String)
    
    /**
     * Log a channel access event.
     */
    fun logChannelAccess(username: String, ipAddress: String, channelId: String, action: String)
    
    /**
     * Log a rate limit exceeded event.
     */
    fun logRateLimitExceeded(username: String?, ipAddress: String)
    
    /**
     * Log a security configuration change event.
     */
    fun logSecurityConfigChange(username: String, ipAddress: String, details: String)
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