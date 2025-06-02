package io.github.mpecan.pmt.metrics.config

import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.metrics.MicrometerMetricsService
import io.github.mpecan.pmt.metrics.NoOpMetricsService
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for MetricsService.
 * Automatically configures the appropriate implementation based on classpath availability.
 */
@AutoConfiguration
class MetricsAutoConfiguration {
    private val logger = LoggerFactory.getLogger(MetricsAutoConfiguration::class.java)

    /**
     * Configuration when Micrometer is available on the classpath.
     */
    @Configuration
    @ConditionalOnClass(MeterRegistry::class)
    class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean
        fun metricsService(meterRegistry: MeterRegistry): MetricsService {
            LoggerFactory
                .getLogger(MicrometerConfiguration::class.java)
                .info("Configuring MicrometerMetricsService - metrics collection enabled")
            return MicrometerMetricsService(meterRegistry)
        }
    }

    /**
     * Configuration when Micrometer is NOT available on the classpath.
     */
    @Configuration
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    class NoOpConfiguration {
        @Bean
        @ConditionalOnMissingBean
        fun metricsService(): MetricsService {
            LoggerFactory
                .getLogger(NoOpConfiguration::class.java)
                .info("Configuring NoOpMetricsService - metrics collection disabled (Micrometer not found)")
            return NoOpMetricsService()
        }
    }
}
