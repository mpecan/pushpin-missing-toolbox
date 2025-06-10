package io.github.mpecan.pmt.security.audit.config

import io.github.mpecan.pmt.security.audit.DefaultAuditLogService
import io.github.mpecan.pmt.security.core.AuditService
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuditAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration::class.java))

    @Test
    fun `should create DefaultAuditLogService when audit is enabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.audit.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(AuditService::class.java))
                assertTrue(context.getBean(AuditService::class.java) is DefaultAuditLogService)
            }
    }

    @Test
    fun `should not create AuditService when audit is disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.audit.enabled=false")
            .run { context ->
                assertTrue(context.getBeansOfType(AuditService::class.java).isEmpty())
            }
    }

    @Test
    fun `should not create AuditService when audit property is not set`() {
        contextRunner
            .run { context ->
                assertTrue(context.getBeansOfType(AuditService::class.java).isEmpty())
            }
    }

    @Test
    fun `should not override existing AuditService bean`() {
        contextRunner
            .withPropertyValues("pushpin.security.audit.enabled=true")
            .withUserConfiguration(ExistingAuditServiceConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(AuditService::class.java))
                assertTrue(context.getBean(AuditService::class.java) is TestAuditService)
            }
    }

    @org.springframework.context.annotation.Configuration
    class ExistingAuditServiceConfiguration {
        @org.springframework.context.annotation.Bean
        fun auditService(): AuditService = TestAuditService()
    }

    class TestAuditService : AuditService {
        override fun log(event: io.github.mpecan.pmt.security.core.AuditEvent) {}

        override fun logAuthSuccess(
            username: String,
            ipAddress: String,
            details: String,
        ) {}

        override fun logAuthFailure(
            username: String,
            ipAddress: String,
            details: String,
        ) {}

        override fun logAuthorizationFailure(
            username: String,
            ipAddress: String,
            resource: String,
            permission: String,
        ) {}

        override fun logChannelAccess(
            username: String,
            ipAddress: String,
            channelId: String,
            action: String,
        ) {}

        override fun logRateLimitExceeded(
            username: String?,
            ipAddress: String,
        ) {}

        override fun logSecurityConfigChange(
            username: String,
            ipAddress: String,
            details: String,
        ) {}

        override fun logRemoteAuthorizationCheck(
            username: String,
            ipAddress: String,
            channelId: String,
            authorized: Boolean,
            source: String,
            duration: Long?,
        ) {
        }

        override fun logRemoteAuthorizationError(
            username: String,
            ipAddress: String,
            channelId: String?,
            error: String,
        ) {}

        override fun logChannelListRetrieval(
            username: String,
            ipAddress: String,
            channelCount: Int,
            source: String,
            duration: Long?,
            pattern: String?,
        ) {
        }
    }
}
