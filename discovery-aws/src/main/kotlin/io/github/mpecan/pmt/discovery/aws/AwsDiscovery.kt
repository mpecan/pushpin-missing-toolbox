package io.github.mpecan.pmt.discovery.aws

import io.github.mpecan.pmt.discovery.PushpinDiscovery
import io.github.mpecan.pmt.discovery.aws.converter.DefaultInstanceConverter
import io.github.mpecan.pmt.discovery.aws.converter.InstanceConverter
import io.github.mpecan.pmt.discovery.aws.health.DefaultInstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.health.InstanceHealthChecker
import io.github.mpecan.pmt.discovery.aws.instances.DefaultEc2InstancesProvider
import io.github.mpecan.pmt.discovery.aws.instances.Ec2InstancesProvider
import io.github.mpecan.pmt.model.PushpinServer
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import software.amazon.awssdk.services.ec2.model.Instance
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A discovery mechanism that discovers Pushpin servers from AWS EC2 instances.
 * 
 * This implementation uses the AWS SDK to discover EC2 instances with specific tags 
 * and converts them to PushpinServer instances. It supports filtering by tags
 * and can be configured to use either public or private IP addresses.
 * 
 * It also supports discovering EC2 instances that are part of Auto Scaling Groups.
 */
open class AwsDiscovery(
    private val properties: AwsDiscoveryProperties,
    private val instancesProvider: Ec2InstancesProvider = DefaultEc2InstancesProvider(),
    private val instanceHealthChecker: InstanceHealthChecker = DefaultInstanceHealthChecker(),
    private val instanceConverter: InstanceConverter = DefaultInstanceConverter()
) : PushpinDiscovery {

    private val logger = LoggerFactory.getLogger(AwsDiscovery::class.java)

    override val id: String = "aws"
    
    // Cache of EC2 instances and last refresh time
    private val instanceCache = ConcurrentHashMap<String, Instance>()
    private var lastCacheRefresh: Instant = Instant.EPOCH
    
    /**
     * Discovers Pushpin servers from AWS EC2 instances based on tags.
     */
    override fun discoverServers(): Flux<PushpinServer> {
        if (!isEnabled()) {
            logger.debug("AWS discovery is disabled")
            return Flux.empty()
        }
        
        logger.debug("Discovering Pushpin servers from AWS EC2 instances")
        
        return Flux.defer {
            refreshInstanceCacheIfNeeded()
            
            Flux.fromIterable(instanceCache.values)
                .filter { instance -> instanceHealthChecker.isHealthy(instance, properties) }
                .map { instance -> instanceConverter.toPushpinServer(instance, properties) }
                .doOnNext { server -> 
                    logger.debug("Discovered Pushpin server from AWS: ${server.id} at ${server.getBaseUrl()}")
                }
                .doOnError { error ->
                    logger.error("Error discovering Pushpin servers from AWS: ${error.message}", error)
                }
        }
    }

    /**
     * Refreshes the instance cache if needed based on the configured refresh interval.
     */
    private fun refreshInstanceCacheIfNeeded() {
        val now = Instant.now()
        val cacheExpiration = lastCacheRefresh.plus(Duration.ofMinutes(properties.refreshCacheMinutes.toLong()))
        
        if (now.isAfter(cacheExpiration)) {
            logger.debug("Refreshing EC2 instance cache")
            try {
                instanceCache.clear()
                
                // Discover instances
                val instances = instancesProvider.getInstances(properties)
                instances.forEach { instance ->
                    instanceCache[instance.instanceId()] = instance
                }
                
                lastCacheRefresh = now
                logger.debug("EC2 instance cache refreshed - found ${instanceCache.size} instances")
            } catch (e: Exception) {
                logger.error("Failed to refresh EC2 instance cache: ${e.message}", e)
                // Don't update lastCacheRefresh so we'll try again on the next call
            }
        }
    }

    override fun isEnabled(): Boolean {
        return properties.enabled
    }
}