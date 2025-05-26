package io.github.mpecan.pmt.security.audit

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.mpecan.pmt.security.core.AuditEvent
import io.github.mpecan.pmt.security.core.AuditEventType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DefaultAuditLogServiceTest {

    private lateinit var auditService: DefaultAuditLogService
    private lateinit var properties: AuditProperties
    private lateinit var listAppender: ListAppender<ILoggingEvent>
    private lateinit var logger: Logger

    @BeforeEach
    fun setUp() {
        properties = AuditProperties(
            enabled = true,
            level = "INFO",
            logSuccessfulAuth = true,
            logFailedAuth = true,
            logChannelAccess = true,
            logAdminActions = true,
        )

        auditService = DefaultAuditLogService(properties)

        // Setup logger to capture log output
        logger = LoggerFactory.getLogger("audit") as Logger
        listAppender = ListAppender()
        listAppender.start()
        logger.addAppender(listAppender)
        logger.level = Level.TRACE
    }

    @Test
    fun `should log authentication success when enabled`() {
        auditService.logAuthSuccess("testuser", "192.168.1.1", "Login via JWT")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertEquals(Level.INFO, logEvent.level)
        assertTrue(logEvent.message.contains("AUTHENTICATION_SUCCESS"))
        assertTrue(logEvent.message.contains("testuser"))
        assertTrue(logEvent.message.contains("192.168.1.1"))
        assertTrue(logEvent.message.contains("Login via JWT"))
    }

    @Test
    fun `should not log authentication success when disabled`() {
        properties = properties.copy(logSuccessfulAuth = false)
        auditService = DefaultAuditLogService(properties)

        auditService.logAuthSuccess("testuser", "192.168.1.1", "Login via JWT")

        assertTrue(listAppender.list.isEmpty())
    }

    @Test
    fun `should log authentication failure`() {
        auditService.logAuthFailure("testuser", "192.168.1.1", "Invalid credentials")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertEquals(Level.INFO, logEvent.level)
        assertTrue(logEvent.message.contains("AUTHENTICATION_FAILURE"))
        assertTrue(logEvent.message.contains("Invalid credentials"))
    }

    @Test
    fun `should log authorization failure`() {
        auditService.logAuthorizationFailure("testuser", "192.168.1.1", "channel1", "subscribe")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertTrue(logEvent.message.contains("AUTHORIZATION_FAILURE"))
        assertTrue(logEvent.message.contains("channel1"))
        assertTrue(logEvent.message.contains("subscribe"))
    }

    @Test
    fun `should log channel access when enabled`() {
        auditService.logChannelAccess("testuser", "192.168.1.1", "channel1", "subscribed")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertTrue(logEvent.message.contains("CHANNEL_ACCESS"))
        assertTrue(logEvent.message.contains("channel1"))
        assertTrue(logEvent.message.contains("subscribed"))
    }

    @Test
    fun `should not log channel access when disabled`() {
        properties = properties.copy(logChannelAccess = false)
        auditService = DefaultAuditLogService(properties)

        auditService.logChannelAccess("testuser", "192.168.1.1", "channel1", "subscribed")

        assertTrue(listAppender.list.isEmpty())
    }

    @Test
    fun `should log rate limit exceeded`() {
        auditService.logRateLimitExceeded("testuser", "192.168.1.1")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertTrue(logEvent.message.contains("RATE_LIMIT_EXCEEDED"))
        assertTrue(logEvent.message.contains("Rate limit exceeded"))
    }

    @Test
    fun `should log rate limit exceeded for anonymous user`() {
        auditService.logRateLimitExceeded(null, "192.168.1.1")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertTrue(logEvent.message.contains("RATE_LIMIT_EXCEEDED"))
        assertTrue(!logEvent.message.contains("User: null"))
    }

    @Test
    fun `should log security config change when admin actions enabled`() {
        auditService.logSecurityConfigChange("admin", "192.168.1.1", "Updated JWT secret")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val logEvent = logEvents[0]
        assertTrue(logEvent.message.contains("SECURITY_CONFIG_CHANGE"))
        assertTrue(logEvent.message.contains("Updated JWT secret"))
    }

    @Test
    fun `should not log security config change when admin actions disabled`() {
        properties = properties.copy(logAdminActions = false)
        auditService = DefaultAuditLogService(properties)

        auditService.logSecurityConfigChange("admin", "192.168.1.1", "Updated JWT secret")

        assertTrue(listAppender.list.isEmpty())
    }

    @Test
    fun `should respect log level setting`() {
        properties = properties.copy(level = "WARN")
        auditService = DefaultAuditLogService(properties)

        auditService.logAuthSuccess("testuser", "192.168.1.1", "")

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)
        assertEquals(Level.WARN, logEvents[0].level)
    }

    @Test
    fun `should not log when disabled`() {
        properties = properties.copy(enabled = false)
        auditService = DefaultAuditLogService(properties)

        auditService.logAuthSuccess("testuser", "192.168.1.1", "")
        auditService.logAuthFailure("testuser", "192.168.1.1", "")
        auditService.logChannelAccess("testuser", "192.168.1.1", "channel1", "subscribed")

        assertTrue(listAppender.list.isEmpty())
    }

    @Test
    fun `should format event with all fields`() {
        val event = AuditEvent(
            type = AuditEventType.AUTHENTICATION_SUCCESS,
            username = "testuser",
            ipAddress = "192.168.1.1",
            details = "Login successful",
        )

        auditService.log(event)

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val message = logEvents[0].message
        assertTrue(message.contains("AUDIT: AUTHENTICATION_SUCCESS"))
        assertTrue(message.contains("User: testuser"))
        assertTrue(message.contains("Login successful"))
        assertTrue(message.contains("IP: 192.168.1.1"))
        assertTrue(message.contains("Time:"))
    }

    @Test
    fun `should format event without username`() {
        val event = AuditEvent(
            type = AuditEventType.RATE_LIMIT_EXCEEDED,
            username = null,
            ipAddress = "192.168.1.1",
            details = "Rate limit exceeded",
        )

        auditService.log(event)

        val logEvents = listAppender.list
        assertEquals(1, logEvents.size)

        val message = logEvents[0].message
        assertTrue(!message.contains("User: null"))
        assertTrue(message.contains("IP: 192.168.1.1"))
    }
}
