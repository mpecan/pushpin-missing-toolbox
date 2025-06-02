package io.github.mpecan.pmt.security.jwt.config

import io.github.mpecan.pmt.security.core.ChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.core.ClaimExtractorService
import io.github.mpecan.pmt.security.core.JwtDecoderService
import io.github.mpecan.pmt.security.core.NoOpClaimExtractorService
import io.github.mpecan.pmt.security.core.NoOpJwtDecoderService
import io.github.mpecan.pmt.security.jwt.DefaultChannelSubscriptionExtractorService
import io.github.mpecan.pmt.security.jwt.DefaultJwtDecoderService
import io.github.mpecan.pmt.security.jwt.JsonPathClaimExtractorService
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtAutoConfiguration::class.java))

    @Test
    fun `should create DefaultJwtDecoderService when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.jwt.enabled=true",
                "pushpin.security.jwt.provider=symmetric",
                "pushpin.security.jwt.secret=test-secret-key-32-characters-long",
            ).run { context ->
                val jwtDecoderService = context.getBean(JwtDecoderService::class.java)
                assertNotNull(jwtDecoderService)
                assertTrue(jwtDecoderService is DefaultJwtDecoderService)
                assertTrue(jwtDecoderService.isJwtEnabled())
            }
    }

    @Test
    fun `should create NoOpJwtDecoderService when disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.jwt.enabled=false")
            .run { context ->
                val jwtDecoderService = context.getBean(JwtDecoderService::class.java)
                assertNotNull(jwtDecoderService)
                assertTrue(jwtDecoderService is NoOpJwtDecoderService)
                assertFalse(jwtDecoderService.isJwtEnabled())
            }
    }

    @Test
    fun `should create ClaimExtractorService when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.jwt.enabled=true",
                "pushpin.security.jwt.provider=symmetric",
                "pushpin.security.jwt.secret=test-secret-key-32-characters-long",
            ).run { context ->
                val claimExtractorService = context.getBean(ClaimExtractorService::class.java)
                assertNotNull(claimExtractorService)
                assertTrue(claimExtractorService is JsonPathClaimExtractorService)
            }
    }

    @Test
    fun `should create NoOpClaimExtractorService when disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.jwt.enabled=false")
            .run { context ->
                val claimExtractorService = context.getBean(ClaimExtractorService::class.java)
                assertNotNull(claimExtractorService)
                assertTrue(claimExtractorService is NoOpClaimExtractorService)
            }
    }

    @Test
    fun `should create ChannelSubscriptionExtractorService when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.jwt.enabled=true",
                "pushpin.security.jwt.provider=symmetric",
                "pushpin.security.jwt.secret=test-secret-key-32-characters-long",
            ).run { context ->
                val extractorService = context.getBean(ChannelSubscriptionExtractorService::class.java)
                assertNotNull(extractorService)
                assertTrue(extractorService is DefaultChannelSubscriptionExtractorService)
            }
    }

    @Test
    fun `should create JWT authentication converter when enabled`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.jwt.enabled=true",
                "pushpin.security.jwt.provider=symmetric",
                "pushpin.security.jwt.secret=test-secret-key-32-characters-long",
            ).run { context ->
                assertTrue(context.containsBean("jwtAuthenticationConverter"))
            }
    }
}
