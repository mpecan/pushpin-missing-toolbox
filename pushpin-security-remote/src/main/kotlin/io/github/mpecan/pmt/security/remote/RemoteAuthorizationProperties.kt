package io.github.mpecan.pmt.security.remote

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for remote authorization.
 */
@ConfigurationProperties(prefix = "pushpin.security.remote")
data class RemoteAuthorizationProperties(
    /**
     * Whether remote authorization is enabled.
     */
    val enabled: Boolean = false,
    /**
     * Base URL of the remote authorization service.
     */
    val url: String = "",
    /**
     * HTTP method to use for remote authorization calls (GET or POST).
     */
    val method: String = "POST",
    /**
     * Connection timeout in milliseconds.
     */
    val timeout: Long = 5000,
    /**
     * Headers to include from the original request when calling remote service.
     */
    val includeHeaders: List<String> = listOf("Authorization", "X-Request-ID"),
    /**
     * Cache configuration for authorization decisions.
     */
    val cache: CacheProperties = CacheProperties(),
) {
    data class CacheProperties(
        /**
         * Whether caching is enabled for authorization decisions.
         */
        val enabled: Boolean = true,
        /**
         * TTL for cache entries in milliseconds. Default: 5 minutes
         */
        val ttl: Long = 300000,
        /**
         * Maximum size of the cache.
         */
        val maxSize: Long = 10000,
    )
}
