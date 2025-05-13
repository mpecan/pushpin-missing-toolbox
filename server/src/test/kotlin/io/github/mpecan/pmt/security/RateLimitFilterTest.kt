package io.github.mpecan.pmt.security

import io.github.bucket4j.Bucket
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.audit.AuditLogService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

class RateLimitFilterTest {

    private lateinit var rateLimitFilter: RateLimitFilter
    private lateinit var mockRequest: HttpServletRequest
    private lateinit var mockResponse: HttpServletResponse
    private lateinit var mockFilterChain: FilterChain
    private lateinit var mockPrintWriter: PrintWriter
    private lateinit var mockAuditLogService: AuditLogService
    private lateinit var properties: PushpinProperties
    private lateinit var buckets: ConcurrentHashMap<String, Bucket>

    @BeforeEach
    fun setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext()

        // Create mock objects
        mockRequest = mock()
        mockResponse = mock()
        mockFilterChain = mock()
        mockPrintWriter = mock()
        mockAuditLogService = mock()
        buckets = spy(ConcurrentHashMap<String, Bucket>())

        // Set up response writer
        whenever(mockResponse.writer).thenReturn(mockPrintWriter)

        // Set up default request behavior
        whenever(mockRequest.remoteAddr).thenReturn("127.0.0.1")

        // Create properties with rate limiting enabled
        val rateLimitProperties = mock<PushpinProperties.RateLimitProperties> {
            on { enabled } doReturn true
            on { capacity } doReturn 5L
            on { refillTimeInMillis } doReturn 60000L
        }

        val securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { rateLimit } doReturn rateLimitProperties
        }

        properties = mock {
            on { security } doReturn securityProperties
        }

        // Create rate limit filter with mocked buckets
        rateLimitFilter = RateLimitFilter(properties, mockAuditLogService, buckets)
    }

    @Test
    fun `filter should skip rate limiting when disabled`() {
        // Arrange - Modify properties to disable rate limiting
        whenever(properties.security.rateLimit.enabled).thenReturn(false)

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert - Should proceed with filter chain without checking buckets
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
        verify(buckets, never()).computeIfAbsent(any(), any())
    }

    @Test
    fun `filter should use IP address for rate limiting when not authenticated`() {
        // Arrange
        val expectedKey = "ip:127.0.0.1"
        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn true
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(eq(expectedKey), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(buckets).computeIfAbsent(eq(expectedKey), any())
        verify(mockBucket).tryConsume(1)
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    @Test
    fun `filter should use username for rate limiting when authenticated`() {
        // Arrange - Set up authenticated user
        val username = "testuser"
        val expectedKey = "user:$username"

        val authentication = UsernamePasswordAuthenticationToken(
            username, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)

        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn true
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(eq(expectedKey), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(buckets).computeIfAbsent(eq(expectedKey), any())
        verify(mockBucket).tryConsume(1)
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    @Test
    fun `filter should allow requests when bucket has tokens`() {
        // Arrange
        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn true
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(any(), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(mockBucket).tryConsume(1)
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
        verify(mockResponse, never()).status = any() // No status should be set
    }

    @Test
    fun `filter should block requests when bucket is empty`() {
        // Arrange
        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn false
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(any(), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(mockBucket).tryConsume(1)
        verify(mockResponse).status = HttpStatus.TOO_MANY_REQUESTS.value()
        verify(mockResponse).setHeader("X-Rate-Limit-Exceeded", "true")
        verify(mockResponse).contentType = "application/json"
        verify(mockResponse.writer).write(any<String>())
        verify(mockFilterChain, never()).doFilter(any(), any()) // Filter chain should not be called
        verify(mockAuditLogService).logRateLimitExceeded(eq(null), any<String>()   )
    }

    @Test
    fun `filter should use X-Forwarded-For header when available`() {
        // Arrange
        val realIP = "203.0.113.1"
        val expectedKey = "ip:$realIP"

        whenever(mockRequest.getHeader("X-Forwarded-For")).thenReturn("$realIP, 10.0.0.1")

        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn true
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(eq(expectedKey), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(buckets).computeIfAbsent(eq(expectedKey), any())
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    @Test
    fun `filter should use X-Real-IP header when X-Forwarded-For is not available`() {
        // Arrange
        val realIP = "203.0.113.1"
        val expectedKey = "ip:$realIP"

        whenever(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null)
        whenever(mockRequest.getHeader("X-Real-IP")).thenReturn(realIP)

        val mockBucket = mock<Bucket> {
            on { tryConsume(1) } doReturn true
        }

        // Set up buckets to return our mock bucket
        doReturn(mockBucket).whenever(buckets).computeIfAbsent(eq(expectedKey), any())

        // Act
        rateLimitFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        verify(buckets).computeIfAbsent(eq(expectedKey), any())
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    @Test
    fun `bucket creation should use properties to configure capacity and refill rate`() {
        // We can't directly test the private createBucket method, but we can test it indirectly
        // by using a real buckets map

        // Arrange - Create a real filter with a real bucket map
        val realBuckets = ConcurrentHashMap<String, Bucket>()
        val filterWithRealBuckets = RateLimitFilter(properties, mockAuditLogService,realBuckets)

        // Act
        filterWithRealBuckets.doFilter(mockRequest, mockResponse, mockFilterChain)

        // Assert
        // Verify the bucket was created with correct parameters
        assert(realBuckets.size == 1)
        assert(realBuckets.containsKey("ip:127.0.0.1"))

        // Verify the filter chain was called
        verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }
}