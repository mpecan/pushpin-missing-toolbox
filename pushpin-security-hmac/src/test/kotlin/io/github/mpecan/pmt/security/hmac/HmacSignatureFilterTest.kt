package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.security.core.AuditService
import io.github.mpecan.pmt.security.core.HmacService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Test version of HmacSignatureFilter to expose protected methods
class TestHmacSignatureFilter(
    hmacService: HmacService,
    properties: HmacProperties,
    auditService: AuditService,
) : HmacSignatureFilter(hmacService, properties, auditService) {
    public override fun shouldNotFilter(request: HttpServletRequest): Boolean = super.shouldNotFilter(request)

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        super.doFilterInternal(request, response, filterChain)
    }
}

class HmacSignatureFilterTest {
    private val hmacService: HmacService = mock()
    private val auditService: AuditService = mock()
    private val properties =
        HmacProperties(
            enabled = true,
            excludedPaths = listOf("/public/", "/health/"),
        )

    private val filter = TestHmacSignatureFilter(hmacService, properties, auditService)
    private val filterChain: FilterChain = mock()

    @Test
    fun `should skip filter when HMAC is disabled`() {
        whenever(hmacService.isHmacEnabled()).thenReturn(false)

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"

        val shouldNotFilter = filter.shouldNotFilter(request)

        assertTrue(shouldNotFilter)
    }

    @Test
    fun `should skip filter for excluded paths`() {
        whenever(hmacService.isHmacEnabled()).thenReturn(true)

        val request = MockHttpServletRequest()
        request.requestURI = "/public/api"

        val shouldNotFilter = filter.shouldNotFilter(request)

        assertTrue(shouldNotFilter)
    }

    @Test
    fun `should not skip filter for non-excluded paths`() {
        whenever(hmacService.isHmacEnabled()).thenReturn(true)

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"

        val shouldNotFilter = filter.shouldNotFilter(request)

        assertFalse(shouldNotFilter)
    }

    @Test
    fun `should reject request without signature header`() {
        whenever(hmacService.getHeaderName()).thenReturn("X-Signature")

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        request.remoteAddr = "127.0.0.1"
        // No signature header
        request.addHeader("X-Pushpin-Timestamp", System.currentTimeMillis().toString())

        val response = MockHttpServletResponse()

        filter.doFilterInternal(request, response, filterChain)

        assertEquals(401, response.status)
        verify(auditService).logAuthorizationFailure(
            "server-to-server",
            "127.0.0.1",
            "/api/test",
            "HMAC_SIGNATURE",
        )
        verify(filterChain, never()).doFilter(any(), any())
    }

    @Test
    fun `should reject request without timestamp header`() {
        whenever(hmacService.getHeaderName()).thenReturn("X-Signature")

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        request.remoteAddr = "127.0.0.1"
        request.addHeader("X-Signature", "signature")
        // No timestamp header

        val response = MockHttpServletResponse()

        filter.doFilterInternal(request, response, filterChain)

        assertEquals(401, response.status)
        verify(auditService).logAuthorizationFailure(
            "server-to-server",
            "127.0.0.1",
            "/api/test",
            "HMAC_SIGNATURE",
        )
        verify(filterChain, never()).doFilter(any(), any())
    }

    @Test
    fun `should process request with valid headers`() {
        whenever(hmacService.getHeaderName()).thenReturn("X-Signature")
        whenever(hmacService.verifyRequestSignature(any(), any(), any(), any(), any())).thenReturn(true)

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        request.remoteAddr = "127.0.0.1"
        request.addHeader("X-Signature", "valid-signature")
        request.addHeader("X-Pushpin-Timestamp", System.currentTimeMillis().toString())
        request.setContent("{}".toByteArray())

        val response = MockHttpServletResponse()

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(any(), eq(response))
        verify(hmacService).verifyRequestSignature(any(), any(), any(), any(), any())
        verify(auditService, never()).logAuthorizationFailure(any(), any(), any(), any())
    }

    @Test
    fun `should log audit event for invalid signature`() {
        whenever(hmacService.getHeaderName()).thenReturn("X-Signature")
        whenever(hmacService.verifyRequestSignature(any(), any(), any(), any(), any())).thenReturn(false)

        val request = MockHttpServletRequest()
        request.requestURI = "/api/test"
        request.remoteAddr = "127.0.0.1"
        request.addHeader("X-Signature", "invalid-signature")
        request.addHeader("X-Pushpin-Timestamp", System.currentTimeMillis().toString())
        request.setContent("{}".toByteArray())

        val response = MockHttpServletResponse()

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(any(), eq(response))
        verify(auditService).logAuthorizationFailure(
            "server-to-server",
            "127.0.0.1",
            "/api/test",
            "HMAC_SIGNATURE",
        )
    }
}
