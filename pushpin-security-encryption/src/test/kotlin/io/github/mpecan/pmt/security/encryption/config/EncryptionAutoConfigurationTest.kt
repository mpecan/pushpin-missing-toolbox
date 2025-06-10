package io.github.mpecan.pmt.security.encryption.config

import io.github.mpecan.pmt.security.core.EncryptionService
import io.github.mpecan.pmt.security.encryption.DefaultEncryptionService
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptionAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EncryptionAutoConfiguration::class.java))

    @Test
    fun `should create DefaultEncryptionService when encryption is enabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.encryption.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(EncryptionService::class.java))
                assertTrue(context.getBean(EncryptionService::class.java) is DefaultEncryptionService)
            }
    }

    @Test
    fun `should not create EncryptionService when encryption is disabled`() {
        contextRunner
            .withPropertyValues("pushpin.security.encryption.enabled=false")
            .run { context ->
                assertTrue(context.getBeansOfType(EncryptionService::class.java).isEmpty())
            }
    }

    @Test
    fun `should not create EncryptionService when encryption property is not set`() {
        contextRunner
            .run { context ->
                assertTrue(context.getBeansOfType(EncryptionService::class.java).isEmpty())
            }
    }

    @Test
    fun `should not override existing EncryptionService bean`() {
        contextRunner
            .withPropertyValues("pushpin.security.encryption.enabled=true")
            .withUserConfiguration(ExistingEncryptionServiceConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(EncryptionService::class.java))
                assertTrue(context.getBean(EncryptionService::class.java) is TestEncryptionService)
            }
    }

    @org.springframework.context.annotation.Configuration
    class ExistingEncryptionServiceConfiguration {
        @org.springframework.context.annotation.Bean
        fun encryptionService(): EncryptionService = TestEncryptionService()
    }

    class TestEncryptionService : EncryptionService {
        override fun encrypt(plaintext: String): String = "encrypted"

        override fun decrypt(encryptedData: String): String = "decrypted"

        override fun isEncryptionEnabled(): Boolean = true

        override fun generateSecretKey(): String = "test-key"
    }
}
