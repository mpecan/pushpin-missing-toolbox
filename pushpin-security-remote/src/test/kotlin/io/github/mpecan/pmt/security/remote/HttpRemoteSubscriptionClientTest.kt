package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.security.core.AuditService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpRemoteSubscriptionClientTest {

    @Mock
    private lateinit var mockCache: SubscriptionAuthorizationCache

    @Mock
    private lateinit var mockRestTemplate: RestTemplate

    @Mock
    private lateinit var mockRequest: HttpServletRequest

    @Mock
    private lateinit var mockAuditService: AuditService

    private lateinit var httpRemoteSubscriptionClient: HttpRemoteSubscriptionClient
    private lateinit var properties: RemoteAuthorizationProperties

    companion object {
        private const val TEST_USER = "testuser"
        private const val TEST_CHANNEL = "channel1"
        private const val TEST_PATTERN = "news.*"
        private const val TEST_IP = "127.0.0.1"
        private const val AUTH_URL = "https://auth.example.com"
    }

    @BeforeEach
    fun setUp() {
        setupDefaultProperties()
        setupDefaultMocks()
        createClientUnderTest()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun setupDefaultProperties(
        authEnabled: Boolean = true,
        cacheEnabled: Boolean = true,
        method: String = "POST",
        includeHeaders: List<String> = listOf("Authorization"),
    ) {
        properties = RemoteAuthorizationProperties(
            enabled = authEnabled,
            url = AUTH_URL,
            method = method,
            timeout = 5000,
            includeHeaders = includeHeaders,
            cache = RemoteAuthorizationProperties.CacheProperties(
                enabled = cacheEnabled,
                ttl = 300000,
                maxSize = 1000,
            ),
        )
    }

    private fun setupDefaultMocks() {
        // Default cache behavior - no cached results
        whenever(mockCache.getSubscriptionCheck(anyString(), anyString())).thenReturn(null)
        whenever(mockCache.getSubscribableChannels(anyString())).thenReturn(null)
        whenever(mockCache.getSubscribableChannelsByPattern(anyString(), anyString())).thenReturn(null)

        // Default request behavior
        whenever(mockRequest.remoteAddr).thenReturn(TEST_IP)
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Bearer token123")
    }

    private fun createClientUnderTest() {
        httpRemoteSubscriptionClient = HttpRemoteSubscriptionClient(
            properties,
            mockCache,
            mockRestTemplate,
            mockAuditService,
        )
    }

    private fun setupAuthenticatedUser(userId: String = TEST_USER) {
        val auth = TestingAuthenticationToken(userId, null)
        auth.isAuthenticated = true
        SecurityContextHolder.getContext().authentication = auth
    }

    @Nested
    inner class CanSubscribeTests {

        @Test
        fun `should return cached result when cache is enabled and has entry`() {
            setupAuthenticatedUser()
            whenever(mockCache.getSubscriptionCheck(TEST_USER, TEST_CHANNEL)).thenReturn(true)

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertTrue(result)
            verify(mockRestTemplate, never()).exchange(any<URI>(), any(), any<HttpEntity<*>>(), any<Class<*>>())
            verify(mockCache, never()).cacheSubscriptionCheck(any(), any(), any())
        }

        @Test
        fun `should return false when user is not authenticated`() {
            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertFalse(result)
            verify(mockRestTemplate, never()).exchange(any<URI>(), any(), any<HttpEntity<*>>(), any<Class<*>>())
        }

        @Test
        fun `should return false when user is anonymous`() {
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("anonymousUser", null)

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertFalse(result)
        }

        @ParameterizedTest
        @ValueSource(strings = ["GET", "POST"])
        fun `should make HTTP request and cache result when allowed`(method: String) {
            setupDefaultProperties(method = method)
            createClientUnderTest()
            setupAuthenticatedUser()

            val response = HttpRemoteSubscriptionClient.SubscriptionResponse(allowed = true)
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertTrue(result)
            verify(mockCache).cacheSubscriptionCheck(TEST_USER, TEST_CHANNEL, true)
        }

        @Test
        fun `should include configured headers in request`() {
            setupDefaultProperties(includeHeaders = listOf("Authorization", "X-Request-ID"))
            createClientUnderTest()
            setupAuthenticatedUser()

            whenever(mockRequest.getHeader("X-Request-ID")).thenReturn("req-123")
            val response = HttpRemoteSubscriptionClient.SubscriptionResponse(allowed = true)
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            val captor = ArgumentCaptor.forClass(HttpEntity::class.java)
            verify(mockRestTemplate).exchange(
                any<URI>(),
                any(),
                captor.capture(),
                eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
            )

            val headers = captor.value.headers
            assertEquals("Bearer token123", headers.getFirst("Authorization"))
            assertEquals("req-123", headers.getFirst("X-Request-ID"))
        }

        @Test
        fun `should return false on HTTP client error`() {
            setupAuthenticatedUser()
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenThrow(HttpClientErrorException(HttpStatus.FORBIDDEN))

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertFalse(result)
        }

        @Test
        fun `should return false on timeout`() {
            setupAuthenticatedUser()
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenThrow(ResourceAccessException("Connection timeout", SocketTimeoutException()))

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertFalse(result)
        }

        @Test
        fun `should return false when response body is null`() {
            setupAuthenticatedUser()
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(null))

            val result = httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            assertFalse(result)
        }

        @Test
        fun `should not cache when caching is disabled`() {
            setupDefaultProperties(cacheEnabled = false)
            createClientUnderTest()
            setupAuthenticatedUser()

            val response = HttpRemoteSubscriptionClient.SubscriptionResponse(allowed = true)
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.SubscriptionResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            httpRemoteSubscriptionClient.canSubscribe(mockRequest, TEST_CHANNEL)

            verify(mockCache, never()).getSubscriptionCheck(any(), any())
            verify(mockCache, never()).cacheSubscriptionCheck(any(), any(), any())
        }
    }

    @Nested
    inner class GetSubscribableChannelsTests {

        @Test
        fun `should return cached channels when available`() {
            setupAuthenticatedUser()
            val cachedChannels = listOf("channel1", "channel2")
            whenever(mockCache.getSubscribableChannels(TEST_USER)).thenReturn(cachedChannels)

            val result = httpRemoteSubscriptionClient.getSubscribableChannels(mockRequest)

            assertEquals(cachedChannels, result)
            verify(mockRestTemplate, never()).exchange(any<URI>(), any(), any<HttpEntity<*>>(), any<Class<*>>())
        }

        @Test
        fun `should return empty list when user is not authenticated`() {
            val result = httpRemoteSubscriptionClient.getSubscribableChannels(mockRequest)

            assertTrue(result.isEmpty())
        }

        @ParameterizedTest
        @ValueSource(strings = ["GET", "POST"])
        fun `should fetch and cache channels from remote`(method: String) {
            setupDefaultProperties(method = method)
            createClientUnderTest()
            setupAuthenticatedUser()

            val channels = listOf("channel1", "channel2", "channel3")
            val response = HttpRemoteSubscriptionClient.ChannelsResponse(channels)
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.ChannelsResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            val result = httpRemoteSubscriptionClient.getSubscribableChannels(mockRequest)

            assertEquals(channels, result)
            verify(mockCache).cacheSubscribableChannels(TEST_USER, channels)
        }

        @Test
        fun `should return empty list on error`() {
            setupAuthenticatedUser()
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.ChannelsResponse::class.java),
                ),
            ).thenThrow(RuntimeException("Network error"))

            val result = httpRemoteSubscriptionClient.getSubscribableChannels(mockRequest)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `should return empty list when response is null`() {
            setupAuthenticatedUser()
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.ChannelsResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(null))

            val result = httpRemoteSubscriptionClient.getSubscribableChannels(mockRequest)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class GetSubscribableChannelsByPatternTests {

        @Test
        fun `should return cached channels when available`() {
            setupAuthenticatedUser()
            val cachedChannels = listOf("news.sports", "news.tech")
            whenever(mockCache.getSubscribableChannelsByPattern(TEST_USER, TEST_PATTERN))
                .thenReturn(cachedChannels)

            val result = httpRemoteSubscriptionClient.getSubscribableChannelsByPattern(mockRequest, TEST_PATTERN)

            assertEquals(cachedChannels, result)
            verify(mockRestTemplate, never()).exchange(any<URI>(), any(), any<HttpEntity<*>>(), any<Class<*>>())
        }

        @ParameterizedTest
        @ValueSource(strings = ["GET", "POST"])
        fun `should fetch and cache channels by pattern`(method: String) {
            setupDefaultProperties(method = method)
            createClientUnderTest()
            setupAuthenticatedUser()

            val channels = listOf("news.sports", "news.tech", "news.world")
            val response = HttpRemoteSubscriptionClient.ChannelsResponse(channels)
            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.ChannelsResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            val result = httpRemoteSubscriptionClient.getSubscribableChannelsByPattern(mockRequest, TEST_PATTERN)

            assertEquals(channels, result)
            verify(mockCache).cacheSubscribableChannelsByPattern(TEST_USER, TEST_PATTERN, channels)
        }

        @Test
        fun `should handle special characters in pattern`() {
            setupAuthenticatedUser()
            val pattern = "user.*.messages"
            val channels = listOf("user.123.messages", "user.456.messages")
            val response = HttpRemoteSubscriptionClient.ChannelsResponse(channels)

            whenever(
                mockRestTemplate.exchange(
                    any<URI>(),
                    any(),
                    any<HttpEntity<*>>(),
                    eq(HttpRemoteSubscriptionClient.ChannelsResponse::class.java),
                ),
            ).thenReturn(ResponseEntity.ok(response))

            val result = httpRemoteSubscriptionClient.getSubscribableChannelsByPattern(mockRequest, pattern)

            assertEquals(channels, result)
        }
    }
}
