package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.security.core.HmacSignatureException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class DefaultHmacServiceTest {

    private val properties = HmacProperties(
        enabled = true,
        secretKey = "test-secret-key",
        algorithm = "HmacSHA256",
        headerName = "X-Test-Signature",
    )

    private val service = DefaultHmacService(properties)

    @Test
    fun `should generate consistent signatures for same data`() {
        val data = "test data"
        val signature1 = service.generateSignature(data)
        val signature2 = service.generateSignature(data)

        assertEquals(signature1, signature2)
    }

    @Test
    fun `should generate different signatures for different data`() {
        val signature1 = service.generateSignature("data1")
        val signature2 = service.generateSignature("data2")

        assertTrue(signature1 != signature2)
    }

    @Test
    fun `should verify valid signature`() {
        val data = "test data"
        val signature = service.generateSignature(data)

        assertTrue(service.verifySignature(data, signature))
    }

    @Test
    fun `should reject invalid signature`() {
        val data = "test data"
        val invalidSignature = "invalid"

        assertFalse(service.verifySignature(data, invalidSignature))
    }

    @Test
    fun `should generate request signature with all parameters`() {
        val body = "{\"message\":\"test\"}"
        val timestamp = System.currentTimeMillis()
        val path = "/api/test"

        val signature = service.generateRequestSignature(body, timestamp, path)

        assertTrue(signature.isNotEmpty())

        // Should be able to verify it
        val expectedData = "$timestamp:$path:$body"
        assertTrue(service.verifySignature(expectedData, signature))
    }

    @Test
    fun `should verify valid request signature`() {
        val body = "{\"message\":\"test\"}"
        val timestamp = System.currentTimeMillis()
        val path = "/api/test"

        val signature = service.generateRequestSignature(body, timestamp, path)

        assertTrue(
            service.verifyRequestSignature(body, timestamp, path, signature),
        )
    }

    @Test
    fun `should reject request signature if too old`() {
        val body = "{\"message\":\"test\"}"
        val oldTimestamp = System.currentTimeMillis() - 600000 // 10 minutes ago
        val path = "/api/test"

        val signature = service.generateRequestSignature(body, oldTimestamp, path)

        assertFalse(
            service.verifyRequestSignature(body, oldTimestamp, path, signature, 300000),
        )
    }

    @Test
    fun `should reject request signature if data changed`() {
        val body = "{\"message\":\"test\"}"
        val timestamp = System.currentTimeMillis()
        val path = "/api/test"

        val signature = service.generateRequestSignature(body, timestamp, path)

        val modifiedBody = "{\"message\":\"modified\"}"
        assertFalse(
            service.verifyRequestSignature(modifiedBody, timestamp, path, signature),
        )
    }

    @Test
    fun `should return correct configuration values`() {
        assertTrue(service.isHmacEnabled())
        assertEquals("HmacSHA256", service.getAlgorithm())
        assertEquals("X-Test-Signature", service.getHeaderName())
    }

    @Test
    fun `should handle invalid algorithm`() {
        val invalidProperties = properties.copy(algorithm = "InvalidAlgo")
        val invalidService = DefaultHmacService(invalidProperties)

        assertThrows<HmacSignatureException> {
            invalidService.generateSignature("test")
        }
    }
}
