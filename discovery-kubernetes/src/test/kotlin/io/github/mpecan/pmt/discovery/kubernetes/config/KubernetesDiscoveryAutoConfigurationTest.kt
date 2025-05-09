package io.github.mpecan.pmt.discovery.kubernetes.config

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscovery
import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.github.mpecan.pmt.discovery.kubernetes.clients.KubernetesClientProvider
import io.github.mpecan.pmt.discovery.kubernetes.converter.DefaultPodConverter
import io.github.mpecan.pmt.discovery.kubernetes.converter.PodConverter
import io.github.mpecan.pmt.discovery.kubernetes.health.DefaultPodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.health.PodHealthChecker
import io.github.mpecan.pmt.discovery.kubernetes.pods.DefaultKubernetesPodProvider
import io.github.mpecan.pmt.discovery.kubernetes.pods.KubernetesPodProvider
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class KubernetesDiscoveryAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(KubernetesDiscoveryAutoConfiguration::class.java))

    @Test
    fun `should not create beans when disabled`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.kubernetes.enabled=false")
            .run { context ->
                assert(!context.containsBean("kubernetesDiscovery"))
            }
    }

    @Test
    fun `should create default beans when enabled`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.kubernetes.enabled=true")
            .run { context ->
                assertNotNull(context.getBean(KubernetesDiscoveryProperties::class.java))
                assertNotNull(context.getBean(KubernetesClientProvider::class.java))
                assertNotNull(context.getBean(KubernetesPodProvider::class.java))
                assertNotNull(context.getBean(PodHealthChecker::class.java))
                assertNotNull(context.getBean(PodConverter::class.java))
                assertNotNull(context.getBean(KubernetesDiscovery::class.java))
            }
    }

    @Test
    fun `should use custom beans when provided`() {
        contextRunner
            .withPropertyValues("pushpin.discovery.kubernetes.enabled=true")
            .withUserConfiguration(CustomBeansConfiguration::class.java)
            .run { context ->
                assertNotNull(context.getBean(KubernetesDiscoveryProperties::class.java))
                assertNotNull(context.getBean("customClientProvider"))
                assertNotNull(context.getBean("customPodProvider"))
                assertNotNull(context.getBean("customHealthChecker"))
                assertNotNull(context.getBean("customPodConverter"))
                assertNotNull(context.getBean(KubernetesDiscovery::class.java))
            }
    }

    @Configuration
    class CustomBeansConfiguration {
        @Bean
        fun customClientProvider(): KubernetesClientProvider = mock()

        @Bean
        fun customPodProvider(): KubernetesPodProvider = mock()

        @Bean
        fun customHealthChecker(): PodHealthChecker = mock()

        @Bean
        fun customPodConverter(): PodConverter = mock()
    }
}