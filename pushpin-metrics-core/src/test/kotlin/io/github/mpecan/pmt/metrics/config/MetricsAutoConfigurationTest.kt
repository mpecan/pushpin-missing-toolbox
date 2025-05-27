package io.github.mpecan.pmt.metrics.config

import io.github.mpecan.pmt.metrics.MetricsService
import io.github.mpecan.pmt.metrics.MicrometerMetricsService
import io.github.mpecan.pmt.metrics.NoOpMetricsService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetricsAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration::class.java))

    @Test
    fun `should create MicrometerMetricsService when MeterRegistry is available`() {
        contextRunner
            .withUserConfiguration(MeterRegistryConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(MetricsService::class.java))
                assertTrue(context.getBean(MetricsService::class.java) is MicrometerMetricsService)
            }
    }

    @Test
    fun `should create NoOpMetricsService when MeterRegistry is not available`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(MeterRegistry::class.java))
            .run { context ->
                assertNotNull(context.getBean(MetricsService::class.java))
                assertTrue(context.getBean(MetricsService::class.java) is NoOpMetricsService)
            }
    }

    @Test
    fun `should not create MicrometerMetricsService when MeterRegistry is not available`() {
        contextRunner
            .withClassLoader(FilteredClassLoader(MeterRegistry::class.java))
            .run { context ->
                assertTrue(context.getBeansOfType(MicrometerMetricsService::class.java).isEmpty())
            }
    }

    @Configuration
    class MeterRegistryConfiguration {
        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }
}
