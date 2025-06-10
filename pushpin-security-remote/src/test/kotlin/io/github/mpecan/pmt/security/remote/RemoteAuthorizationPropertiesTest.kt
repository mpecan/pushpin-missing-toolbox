package io.github.mpecan.pmt.security.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RemoteAuthorizationPropertiesTest {
    @Test
    fun `should have default values`() {
        val properties = RemoteAuthorizationProperties()

        assertFalse(properties.enabled)
        assertEquals("", properties.url)
        assertEquals("POST", properties.method)
        assertEquals(5000L, properties.timeout)
        assertEquals(listOf("Authorization", "X-Request-ID"), properties.includeHeaders)

        // Check cache properties defaults
        assertTrue(properties.cache.enabled)
        assertEquals(300000L, properties.cache.ttl)
        assertEquals(10000L, properties.cache.maxSize)
    }

    @Test
    fun `should allow custom values`() {
        val cacheProperties =
            RemoteAuthorizationProperties.CacheProperties(
                enabled = false,
                ttl = 600000L,
                maxSize = 5000L,
            )

        val properties =
            RemoteAuthorizationProperties(
                enabled = true,
                url = "https://auth.example.com/api/v1/check",
                method = "GET",
                timeout = 10000L,
                includeHeaders = listOf("Authorization", "X-User-ID", "X-Session-ID"),
                cache = cacheProperties,
            )

        assertTrue(properties.enabled)
        assertEquals("https://auth.example.com/api/v1/check", properties.url)
        assertEquals("GET", properties.method)
        assertEquals(10000L, properties.timeout)
        assertEquals(listOf("Authorization", "X-User-ID", "X-Session-ID"), properties.includeHeaders)

        // Check cache properties
        assertFalse(properties.cache.enabled)
        assertEquals(600000L, properties.cache.ttl)
        assertEquals(5000L, properties.cache.maxSize)
    }

    @Test
    fun `should create copy with modified values`() {
        val original = RemoteAuthorizationProperties()
        val modified =
            original.copy(
                enabled = true,
                url = "https://new-auth.example.com",
                timeout = 8000L,
            )

        // Original should remain unchanged
        assertFalse(original.enabled)
        assertEquals("", original.url)
        assertEquals(5000L, original.timeout)

        // Modified should have new values
        assertTrue(modified.enabled)
        assertEquals("https://new-auth.example.com", modified.url)
        assertEquals(8000L, modified.timeout)

        // Other values should remain the same
        assertEquals(original.method, modified.method)
        assertEquals(original.includeHeaders, modified.includeHeaders)
        assertEquals(original.cache, modified.cache)
    }

    @Test
    fun `should handle cache properties independently`() {
        val customCache =
            RemoteAuthorizationProperties.CacheProperties(
                enabled = false,
                ttl = 120000L,
                maxSize = 2000L,
            )

        val properties = RemoteAuthorizationProperties(cache = customCache)

        // Main properties should have defaults
        assertFalse(properties.enabled)
        assertEquals("POST", properties.method)

        // Cache should have custom values
        assertFalse(properties.cache.enabled)
        assertEquals(120000L, properties.cache.ttl)
        assertEquals(2000L, properties.cache.maxSize)
    }

    @Test
    fun `CacheProperties should have sensible defaults`() {
        val cacheProperties = RemoteAuthorizationProperties.CacheProperties()

        assertTrue(cacheProperties.enabled)
        assertEquals(300000L, cacheProperties.ttl) // 5 minutes
        assertEquals(10000L, cacheProperties.maxSize)
    }

    @Test
    fun `CacheProperties should allow custom values`() {
        val cacheProperties =
            RemoteAuthorizationProperties.CacheProperties(
                enabled = false,
                ttl = 60000L,
                maxSize = 1000L,
            )

        assertFalse(cacheProperties.enabled)
        assertEquals(60000L, cacheProperties.ttl)
        assertEquals(1000L, cacheProperties.maxSize)
    }
}
