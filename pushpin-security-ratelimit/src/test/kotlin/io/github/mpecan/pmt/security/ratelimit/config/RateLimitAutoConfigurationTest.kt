package io.github.mpecan.pmt.security.ratelimit.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.ratelimit.RateLimitFilter
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RateLimitAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration::class.java))
            .withUserConfiguration(MockAuditServiceConfiguration::class.java)

    @Test
    fun `should create RateLimitFilter when rate limiting is enabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.rate-limit.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(RateLimitFilter::class.java))
            }
    }

    @Test
    fun `should not create RateLimitFilter when rate limiting is disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.rate-limit.enabled=false")
            .run { context ->
                assertTrue(context.getBeansOfType(RateLimitFilter::class.java).isEmpty())
            }
    }

    @Test
    fun `should not create RateLimitFilter when rate limiting property is not set`() {
        contextRunner
            .run { context ->
                assertTrue(context.getBeansOfType(RateLimitFilter::class.java).isEmpty())
            }
    }

    @Test
    fun `should respect ConditionalOnMissingBean annotation`() {
        // Test that the auto-configuration respects @ConditionalOnMissingBean
        // by creating context with a pre-existing RateLimitFilter bean
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration::class.java))
            .withPropertyValues("pushpin.security.rate-limit.enabled=true")
            .withUserConfiguration(PreExistingRateLimitFilterConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(RateLimitFilter::class.java))
                // Should only have one bean (the pre-existing one)
                assertTrue(context.getBeansOfType(RateLimitFilter::class.java).size == 1)
            }
    }

    @org.springframework.context.annotation.Configuration
    class MockAuditServiceConfiguration {
        @org.springframework.context.annotation.Bean
        fun auditService(): AuditService = TestAuditService()
    }

    @org.springframework.context.annotation.Configuration
    class PreExistingRateLimitFilterConfiguration {
        @org.springframework.context.annotation.Bean
        fun rateLimitFilter(): RateLimitFilter =
            RateLimitFilter(
                io.github.mpecan.pmt.security.ratelimit
                    .RateLimitProperties(),
                TestAuditService(),
            )

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
