package io.github.mpecan.pmt.security.ratelimit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RateLimitPropertiesTest {
    @Test
    fun `should have default values`() {
        val properties = RateLimitProperties()

        assertFalse(properties.enabled)
        assertEquals(100L, properties.capacity)
        assertEquals(60000L, properties.refillTimeInMillis)
    }

    @Test
    fun `should allow custom values`() {
        val properties =
            RateLimitProperties(
                enabled = true,
                capacity = 50L,
                refillTimeInMillis = 30000L,
            )

        assertEquals(true, properties.enabled)
        assertEquals(50L, properties.capacity)
        assertEquals(30000L, properties.refillTimeInMillis)
    }

    @Test
    fun `should create copy with modified values`() {
        val original = RateLimitProperties()
        val modified = original.copy(enabled = true, capacity = 200L)

        assertFalse(original.enabled)
        assertEquals(100L, original.capacity)
        assertEquals(true, modified.enabled)
        assertEquals(200L, modified.capacity)
        // refillTimeInMillis should remain the same
        assertEquals(original.refillTimeInMillis, modified.refillTimeInMillis)
    }
}
