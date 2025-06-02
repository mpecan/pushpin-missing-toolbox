package io.github.mpecan.pmt.security.core

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NoOpAuditServiceTest {
    private val auditService = NoOpAuditService()

    @Test
    fun `should not throw exceptions when logging events`() {
        // All these calls should complete without throwing exceptions
        auditService.log(
            AuditEvent(
                type = AuditEventType.AUTHENTICATION_SUCCESS,
                username = "testuser",
                ipAddress = "127.0.0.1",
                details = "Test event",
            ),
        )

        auditService.logAuthSuccess("testuser", "127.0.0.1", "Test login")
        auditService.logAuthFailure("testuser", "127.0.0.1", "Invalid password")
        auditService.logAuthorizationFailure("testuser", "127.0.0.1", "resource", "READ")
        auditService.logChannelAccess("testuser", "127.0.0.1", "channel1", "subscribe")
        auditService.logRateLimitExceeded("testuser", "127.0.0.1")
        auditService.logRateLimitExceeded(null, "127.0.0.1")
        auditService.logSecurityConfigChange("admin", "127.0.0.1", "Updated settings")

        // If we get here without exceptions, the test passes
        assertNotNull(auditService)
    }
}
