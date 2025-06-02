package io.github.mpecan.pmt.security.starter

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PushpinSecurityAutoConfigurationTest {
    private val contextRunner =
        WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PushpinSecurityAutoConfiguration::class.java))

    @Test
    fun `should load autoconfiguration without errors`() {
        contextRunner
            .withPropertyValues("pushpin.security.enabled=true")
            .run { context ->
                // Verify the autoconfiguration loads without errors
                assertNotNull(context)
                assertTrue(context.isRunning)
            }
    }

    @Test
    fun `should work with web application context`() {
        contextRunner
            .withPropertyValues(
                "pushpin.security.enabled=true",
                "spring.main.web-application-type=servlet",
            ).run { context ->
                assertNotNull(context)
                assertTrue(context.isRunning)
            }
    }

    @Test
    fun `should load all autoconfiguration classes`() {
        // This test verifies that the autoconfiguration classes can be loaded
        // without throwing ClassNotFoundException
        val autoConfigurations =
            listOf(
                "io.github.mpecan.pmt.security.core.config.SecurityCoreAutoConfiguration",
                "io.github.mpecan.pmt.security.audit.config.AuditAutoConfiguration",
                "io.github.mpecan.pmt.security.encryption.config.EncryptionAutoConfiguration",
                "io.github.mpecan.pmt.security.hmac.config.HmacAutoConfiguration",
                "io.github.mpecan.pmt.security.jwt.config.JwtAutoConfiguration",
            )

        autoConfigurations.forEach { className ->
            assertNotNull(Class.forName(className), "Could not load $className")
        }
    }
}
