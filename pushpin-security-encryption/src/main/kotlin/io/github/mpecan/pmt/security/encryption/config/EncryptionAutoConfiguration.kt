package io.github.mpecan.pmt.security.encryption.config

import io.github.mpecan.pmt.security.core.EncryptionService
import io.github.mpecan.pmt.security.encryption.DefaultEncryptionService
import io.github.mpecan.pmt.security.encryption.EncryptionProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Spring Boot auto-configuration for encryption functionality.
 */
@AutoConfiguration
@EnableConfigurationProperties(EncryptionProperties::class)
@ConditionalOnProperty(
    prefix = "pushpin.security.encryption",
    name = ["enabled"],
    havingValue = "true",
)
class EncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EncryptionService::class)
    fun encryptionService(properties: EncryptionProperties): EncryptionService {
        return DefaultEncryptionService(properties)
    }
}
