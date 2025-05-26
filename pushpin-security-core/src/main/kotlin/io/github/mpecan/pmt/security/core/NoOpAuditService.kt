package io.github.mpecan.pmt.security.core

/**
 * No-op implementation of AuditService that does nothing.
 * This is the default implementation when no other AuditService bean is provided.
 * It ensures that code using AuditService will not fail if no audit module is included.
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

    override fun logRemoteAuthorizationCheck(
        username: String,
        ipAddress: String,
        channelId: String,
        authorized: Boolean,
        source: String,
        duration: Long?,
    ) {
        // No-op
    }

    override fun logRemoteAuthorizationError(username: String, ipAddress: String, channelId: String?, error: String) {
        // No-op
    }

    override fun logChannelListRetrieval(
        username: String,
        ipAddress: String,
        channelCount: Int,
        source: String,
        duration: Long?,
        pattern: String?,
    ) {
        // No-op
    }
}
