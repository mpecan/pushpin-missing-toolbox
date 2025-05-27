package io.github.mpecan.pmt.testcontainers

/**
 * Preset configurations for common Pushpin testing scenarios.
 * * This object provides pre-configured [PushpinConfiguration] instances optimized for
 * specific use cases. These presets help you quickly set up Pushpin containers with
 * appropriate settings for different testing scenarios without having to manually
 * configure all parameters.
 * * Available presets:
 * - [minimal]: Basic HTTP testing with debug enabled
 * - [webSocket]: Optimized for WebSocket connections with increased message rates
 * - [serverSentEvents]: Configured for SSE streaming with longer timeouts
 * - [highThroughput]: Maximum performance settings for load testing
 * - [authenticated]: Security features enabled for auth testing
 * - [productionLike]: Settings that mimic production environments
 * - [development]: Maximum verbosity for debugging
 * * Usage example:
 * ```kotlin
 * val container = PushpinContainerBuilder()
 *     .withPreset(PushpinPresets.webSocket())
 *     .withHostApplicationPort(8080)
 *     .build()
 * ```
 * * @see PushpinConfiguration
 * @see PushpinContainerBuilder
 */
object PushpinPresets {

    /**
     * Minimal configuration for basic HTTP testing.
     * - HTTP only (no HTTPS)
     * - No authentication
     * - Debug mode enabled
     * - Verbose logging
     */
    fun minimal(): PushpinConfiguration = PushpinConfiguration(
        debug = true,
        logLevel = 5,
        updatesCheck = "off",
    )

    /**
     * Configuration for testing WebSocket functionality.
     * - Increased message rates
     * - Longer subscription linger time
     * - Debug mode enabled
     */
    fun webSocket(): PushpinConfiguration = PushpinConfiguration(
        debug = true,
        logLevel = 5,
        messageRate = 5000,
        messageHwm = 50000,
        subscriptionLinger = 120,
        connectionSubscriptionMax = 50,
        updatesCheck = "off",
    )

    /**
     * Configuration for testing Server-Sent Events (SSE).
     * - Optimized for streaming
     * - Longer timeouts
     * - Debug mode enabled
     */
    fun serverSentEvents(): PushpinConfiguration = PushpinConfiguration(
        debug = true,
        logLevel = 5,
        messageWait = 10000,
        subscriptionLinger = 300,
        statsConnectionTtl = 300,
        updatesCheck = "off",
    )

    /**
     * Configuration for high-throughput testing.
     * - Maximum message rates
     * - Large buffers
     * - Maximum connections
     */
    fun highThroughput(): PushpinConfiguration = PushpinConfiguration(
        debug = false,
        logLevel = 2,
        messageRate = 10000,
        messageHwm = 100000,
        clientMaxConn = 100000,
        clientBufferSize = 16384,
        connectionSubscriptionMax = 100,
        updatesCheck = "off",
    )

    /**
     * Configuration with authentication enabled.
     * - Signature verification
     * - CORS enabled
     * - Debug mode for troubleshooting
     */
    fun authenticated(): PushpinConfiguration = PushpinConfiguration(
        debug = true,
        logLevel = 5,
        autoCrossOrigin = true,
        sigKey = "test-secret-key",
        sigIss = "test-issuer",
        acceptXForwardedProtocol = true,
        updatesCheck = "off",
    )

    /**
     * Configuration for production-like testing.
     * - HTTPS enabled (requires cert configuration)
     * - Compression enabled
     * - Standard logging
     * - Security headers
     */
    fun productionLike(): PushpinConfiguration = PushpinConfiguration(
        httpsPort = 8443,
        debug = false,
        logLevel = 2,
        allowCompression = true,
        acceptXForwardedProtocol = true,
        setXForwardedProtocol = "append",
        xForwardedFor = "truncate:0,append",
        logFrom = true,
        logUserAgent = true,
        updatesCheck = "off",
    )

    /**
     * Configuration for development/debugging.
     * - Maximum verbosity
     * - All logging enabled
     * - Debug responses
     */
    fun development(): PushpinConfiguration = PushpinConfiguration(
        debug = true,
        logLevel = 10,
        logFrom = true,
        logUserAgent = true,
        statsConnectionSend = true,
        updatesCheck = "off",
    )
}
