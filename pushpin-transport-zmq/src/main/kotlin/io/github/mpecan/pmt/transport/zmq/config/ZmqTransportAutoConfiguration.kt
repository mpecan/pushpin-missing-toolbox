package io.github.mpecan.pmt.transport.zmq.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.transport.zmq.ZmqTransport
import io.github.mpecan.pmt.transport.zmq.ZmqTransportProperties
import io.github.mpecan.pmt.transport.zmq.health.ZmqHealthChecker
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Auto-configuration for ZMQ transport components.
 */
@AutoConfiguration
@EnableConfigurationProperties(ZmqTransportProperties::class)
@EnableScheduling
@ConditionalOnProperty(
    value = ["pushpin.transport"],
    havingValue = "zmq",
    matchIfMissing = false
)
class ZmqTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun zmqTransport(
        zmqTransportProperties: ZmqTransportProperties,
        messageSerializer: MessageSerializer,
        messageSerializationService: MessageSerializationService,
        discoveryManager: PushpinDiscoveryManager
    ): ZmqTransport {
        return ZmqTransport(zmqTransportProperties, messageSerializer, messageSerializationService, discoveryManager)
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun zmqHealthChecker(objectMapper: ObjectMapper): ZmqHealthChecker {
        return ZmqHealthChecker(objectMapper)
    }
}