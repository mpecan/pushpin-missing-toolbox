package io.github.mpecan.pmt.security.hmac.config

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.HmacService
import io.github.mpecan.pmt.security.core.NoOpHmacService
import io.github.mpecan.pmt.security.hmac.DefaultHmacService
import io.github.mpecan.pmt.security.hmac.HmacSignatureFilter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.*

class HmacAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(HmacAutoConfiguration::class.java))
        .withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `should create DefaultHmacService when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.hmac.enabled=true",
                "pushpin.security.hmac.secret-key=test-secret",
            )
            .run { context ->
                val hmacService = context.getBean(HmacService::class.java)
                assertNotNull(hmacService)
                assertTrue(hmacService is DefaultHmacService)
                assertTrue(hmacService.isHmacEnabled())
            }
    }

    @Test
    fun `should create NoOpHmacService when disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.hmac.enabled=false")
            .run { context ->
                val hmacService = context.getBean(HmacService::class.java)
                assertNotNull(hmacService)
                assertTrue(hmacService is NoOpHmacService)
                assertTrue(!hmacService.isHmacEnabled())
            }
    }

    @Test
    fun `should create HmacSignatureFilter when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.hmac.enabled=true",
                "pushpin.security.hmac.secret-key=test-secret",
            )
            .run { context ->
                val filter = context.getBean(HmacSignatureFilter::class.java)
                assertNotNull(filter)
            }
    }

    @Test
    fun `should not create HmacSignatureFilter when disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.hmac.enabled=false")
            .run { context ->
                assertTrue(!context.containsBean("hmacSignatureFilter"))
            }
    }

    @Test
    fun `should use custom algorithm when configured`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.hmac.enabled=true",
                "pushpin.security.hmac.secret-key=test-secret",
                "pushpin.security.hmac.algorithm=HmacSHA512",
            )
            .run { context ->
                val hmacService = context.getBean(HmacService::class.java)
                assertEquals("HmacSHA512", hmacService.getAlgorithm())
            }
    }

    @Configuration
    class TestConfiguration {
        @Bean
        fun auditService(): AuditService = mock()
    }
}
