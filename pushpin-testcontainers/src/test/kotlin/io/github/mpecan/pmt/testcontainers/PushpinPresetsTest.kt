package io.github.mpecan.pmt.testcontainers

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PushpinPresetsTest {

    @Test
    fun `minimal preset should have debug enabled`() {
        val config = PushpinPresets.minimal()

        assertTrue(config.debug)
        assertEquals(5, config.logLevel)
        assertEquals("off", config.updatesCheck)
    }

    @Test
    fun `websocket preset should have increased message limits`() {
        val config = PushpinPresets.webSocket()

        assertEquals(5000, config.messageRate)
        assertEquals(50000, config.messageHwm)
        assertEquals(120, config.subscriptionLinger)
        assertEquals(50, config.connectionSubscriptionMax)
    }

    @Test
    fun `high throughput preset should have maximum performance settings`() {
        val config = PushpinPresets.highThroughput()

        assertFalse(config.debug)
        assertEquals(2, config.logLevel)
        assertEquals(10000, config.messageRate)
        assertEquals(100000, config.messageHwm)
        assertEquals(100000, config.clientMaxConn)
    }

    @Test
    fun `authenticated preset should have security settings`() {
        val config = PushpinPresets.authenticated()

        assertTrue(config.autoCrossOrigin)
        assertEquals("test-secret-key", config.sigKey)
        assertEquals("test-issuer", config.sigIss)
        assertTrue(config.acceptXForwardedProtocol)
    }

    @Test
    fun `production like preset should have HTTPS and compression`() {
        val config = PushpinPresets.productionLike()

        assertNotNull(config.httpsPort)
        assertEquals(8443, config.httpsPort)
        assertTrue(config.allowCompression)
        assertFalse(config.debug)
        assertEquals(2, config.logLevel)
    }

    @Test
    fun `development preset should have maximum verbosity`() {
        val config = PushpinPresets.development()

        assertTrue(config.debug)
        assertEquals(10, config.logLevel)
        assertTrue(config.logFrom)
        assertTrue(config.logUserAgent)
        assertTrue(config.statsConnectionSend)
    }
}
