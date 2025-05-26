package io.github.mpecan.pmt.security.audit

import io.github.mpecan.pmt.security.core.AuditEvent
import io.github.mpecan.pmt.security.core.AuditService

/**
 * No-op implementation of AuditService that does nothing.
 * Used when audit logging is disabled.
 */
class NoOpAuditService : AuditService {
    
    override fun log(event: AuditEvent) {
        // No-op
    }
    
    override fun logAuthSuccess(username: String, ipAddress: String, details: String) {
        // No-op
    }
    
    override fun logAuthFailure(username: String, ipAddress: String, details: String) {
        // No-op
    }
    
    override fun logAuthorizationFailure(username: String, ipAddress: String, resource: String, permission: String) {
        // No-op
    }
    
    override fun logChannelAccess(username: String, ipAddress: String, channelId: String, action: String) {
        // No-op
    }
    
    override fun logRateLimitExceeded(username: String?, ipAddress: String) {
        // No-op
    }
    
    override fun logSecurityConfigChange(username: String, ipAddress: String, details: String) {
        // No-op
    }
}