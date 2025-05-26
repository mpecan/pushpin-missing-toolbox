package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.model.ChannelPermissions
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpRemoteAuthorizationClientTest {

    private lateinit var client: HttpRemoteAuthorizationClient
    private lateinit var pushpinProperties: PushpinProperties
    private lateinit var cache: AuthorizationCache
    private lateinit var auditLogService: AuditLogService
    private lateinit var restTemplate: RestTemplate
    private lateinit var mockRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        // Clear security context
        SecurityContextHolder.clearContext()
        
        // Create mocks
        cache = mock()
        auditLogService = mock()
        restTemplate = mock()
        mockRequest = mock()
        
        // Set up request headers
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Bearer test-token")
        
        // Create mock properties
        val remoteAuthProperties = mock<PushpinProperties.JwtProperties.RemoteAuthorizationProperties> {
            on { enabled } doReturn true
            on { url } doReturn "http://auth-service/api/auth"
            on { method } doReturn "POST"
            on { timeout } doReturn 5000L
            on { cacheEnabled } doReturn true
            on { includeHeaders } doReturn listOf("Authorization")
        }
        
        val jwtProperties = mock<PushpinProperties.JwtProperties> {
            on { remoteAuthorization } doReturn remoteAuthProperties
        }
        
        val securityProperties = mock<PushpinProperties.SecurityProperties> {
            on { jwt } doReturn jwtProperties
        }
        
        pushpinProperties = mock {
            on { security } doReturn securityProperties
        }
        
        // Create client with proper constructor injection
        client = HttpRemoteAuthorizationClient(pushpinProperties, cache, auditLogService, restTemplate)
        
        // Set up authentication
        val authentication = UsernamePasswordAuthenticationToken(
            "testuser", null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `hasPermission should check cache first when caching is enabled`() {
        // Arrange
        val userId = "testuser"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        
        // Setup cache hit
        whenever(cache.getPermissionCheck(userId, channelId, permission)).thenReturn(true)
        
        // Act
        val result = client.hasPermission(mockRequest, channelId, permission)
        
        // Assert
        assertTrue(result)
        
        // Verify cache was checked but no REST call was made
        verify(cache).getPermissionCheck(userId, channelId, permission)
        verifyNoInteractions(restTemplate)
    }
    
    @Test
    fun `hasPermission should use POST method when configured`() {
        // Arrange
        val userId = "testuser"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        
        // Setup cache miss
        whenever(cache.getPermissionCheck(userId, channelId, permission)).thenReturn(null)
        
        // Setup REST response
        val responseEntity = mock<ResponseEntity<HttpRemoteAuthorizationClient.AuthorizationResponse>> {
            on { body } doReturn HttpRemoteAuthorizationClient.AuthorizationResponse(true)
        }
        
        whenever(restTemplate.exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            anyOrNull<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.AuthorizationResponse::class.java)
        )).thenReturn(responseEntity)
        
        // Act
        val result = client.hasPermission(mockRequest, channelId, permission)
        
        // Assert
        assertTrue(result)
        
        // Verify REST call was made with POST
        verify(restTemplate).exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            any<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.AuthorizationResponse::class.java)
        )
        
        // Verify result was cached
        verify(cache).cachePermissionCheck(userId, channelId, permission, true)
    }
    
    @Test
    fun `hasPermission should use GET method when configured`() {
        // Arrange
        val userId = "testuser"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        
        // Configure for GET
        whenever(pushpinProperties.security.jwt.remoteAuthorization.method).thenReturn("GET")
        
        // Setup cache miss
        whenever(cache.getPermissionCheck(userId, channelId, permission)).thenReturn(null)
        
        // Setup REST response
        val responseEntity = mock<ResponseEntity<HttpRemoteAuthorizationClient.AuthorizationResponse>> {
            on { body } doReturn HttpRemoteAuthorizationClient.AuthorizationResponse(true)
        }
        
        whenever(restTemplate.exchange(
            any<java.net.URI>(),
            eq(HttpMethod.GET),
            anyOrNull<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.AuthorizationResponse::class.java)
        )).thenReturn(responseEntity)
        
        // Act
        val result = client.hasPermission(mockRequest, channelId, permission)
        
        // Assert
        assertTrue(result)
        
        // Verify REST call was made with GET
        verify(restTemplate).exchange(
            any<java.net.URI>(),
            eq(HttpMethod.GET),
            any<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.AuthorizationResponse::class.java)
        )
        
        // Verify result was cached
        verify(cache).cachePermissionCheck(userId, channelId, permission, true)
    }
    
    @Test
    fun `hasPermission should log and return false on error`() {
        // Arrange
        val userId = "testuser"
        val channelId = "channel1"
        val permission = ChannelPermission.READ
        
        // Setup cache miss
        whenever(cache.getPermissionCheck(userId, channelId, permission)).thenReturn(null)
        
        // Setup REST error
        whenever(restTemplate.exchange(
            any<java.net.URI>(),
            any(),
            anyOrNull<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.AuthorizationResponse::class.java)
        )).thenThrow(RuntimeException("Test error"))
        
        // Act
        val result = client.hasPermission(mockRequest, channelId, permission)
        
        // Assert
        assertFalse(result)
        
        // Verify error was logged
        verify(auditLogService).logAuthorizationFailure(
            eq(userId),
            eq("remote-request"),
            eq(channelId),
            argThat { contains("ERROR") }
        )
    }
    
    @Test
    fun `getChannelsWithPermission should check cache first when caching is enabled`() {
        // Arrange
        val userId = "testuser"
        val permission = ChannelPermission.READ
        val channels = listOf("channel1", "channel2")
        
        // Setup cache hit
        whenever(cache.getChannelsWithPermission(userId, permission)).thenReturn(channels)
        
        // Act
        val result = client.getChannelsWithPermission(mockRequest, permission)
        
        // Assert
        assertEquals(channels, result)
        
        // Verify cache was checked but no REST call was made
        verify(cache).getChannelsWithPermission(userId, permission)
        verifyNoInteractions(restTemplate)
    }
    
    @Test
    fun `getChannelsWithPermission should use POST method when configured`() {
        // Arrange
        val userId = "testuser"
        val permission = ChannelPermission.READ
        val channels = listOf("channel1", "channel2")
        
        // Setup cache miss
        whenever(cache.getChannelsWithPermission(userId, permission)).thenReturn(null)
        
        // Setup REST response
        val responseEntity = mock<ResponseEntity<HttpRemoteAuthorizationClient.ChannelsResponse>> {
            on { body } doReturn HttpRemoteAuthorizationClient.ChannelsResponse(channels)
        }
        
        whenever(restTemplate.exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            anyOrNull<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.ChannelsResponse::class.java)
        )).thenReturn(responseEntity)
        
        // Act
        val result = client.getChannelsWithPermission(mockRequest, permission)
        
        // Assert
        assertEquals(channels, result)
        
        // Verify REST call was made with POST
        verify(restTemplate).exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            any<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.ChannelsResponse::class.java)
        )
        
        // Verify result was cached
        verify(cache).cacheChannelsWithPermission(userId, permission, channels)
    }
    
    @Test
    fun `getAllChannelPermissions should check cache first when caching is enabled`() {
        // Arrange
        val userId = "testuser"
        val permissions = listOf(
            ChannelPermissions("channel1", setOf(ChannelPermission.READ, ChannelPermission.WRITE)),
            ChannelPermissions("channel2", setOf(ChannelPermission.READ))
        )
        
        // Setup cache hit
        whenever(cache.getAllChannelPermissions(userId)).thenReturn(permissions)
        
        // Act
        val result = client.getAllChannelPermissions(mockRequest)
        
        // Assert
        assertEquals(permissions, result)
        
        // Verify cache was checked but no REST call was made
        verify(cache).getAllChannelPermissions(userId)
        verifyNoInteractions(restTemplate)
    }
    
    @Test
    fun `getAllChannelPermissions should use POST method when configured`() {
        // Arrange
        val userId = "testuser"
        val permissionsMap = mapOf(
            "channel1" to listOf("READ", "WRITE"),
            "channel2" to listOf("READ")
        )
        
        // Setup cache miss
        whenever(cache.getAllChannelPermissions(userId)).thenReturn(null)
        
        // Setup REST response
        val responseEntity = mock<ResponseEntity<HttpRemoteAuthorizationClient.PermissionsResponse>> {
            on { body } doReturn HttpRemoteAuthorizationClient.PermissionsResponse(permissionsMap)
        }
        
        whenever(restTemplate.exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            anyOrNull<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.PermissionsResponse::class.java)
        )).thenReturn(responseEntity)
        
        // Act
        val result = client.getAllChannelPermissions(mockRequest)
        
        // Assert
        assertEquals(2, result.size)
        
        val channel1Perms = result.find { it.channelId == "channel1" }
        assertEquals(2, channel1Perms?.permissions?.size)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        assertTrue(channel1Perms?.permissions?.contains(ChannelPermission.WRITE) ?: false)
        
        val channel2Perms = result.find { it.channelId == "channel2" }
        assertEquals(1, channel2Perms?.permissions?.size)
        assertTrue(channel2Perms?.permissions?.contains(ChannelPermission.READ) ?: false)
        
        // Verify REST call was made with POST
        verify(restTemplate).exchange(
            any<java.net.URI>(),
            eq(HttpMethod.POST),
            any<HttpEntity<*>>(),
            eq(HttpRemoteAuthorizationClient.PermissionsResponse::class.java)
        )
        
        // Verify result was cached
        verify(cache).cacheAllChannelPermissions(eq(userId), any())
    }
    
    @Test
    fun `should return empty results when no authentication is available`() {
        // Arrange - clear security context
        SecurityContextHolder.clearContext()
        
        // Act
        val hasPermResult = client.hasPermission(mockRequest, "channel1", ChannelPermission.READ)
        val channelsResult = client.getChannelsWithPermission(mockRequest, ChannelPermission.READ)
        val permissionsResult = client.getAllChannelPermissions(mockRequest)
        
        // Assert
        assertFalse(hasPermResult)
        assertTrue(channelsResult.isEmpty())
        assertTrue(permissionsResult.isEmpty())
        
        // Verify no REST calls were made
        verifyNoInteractions(restTemplate)
    }
    
    @Test
    fun `should return empty results when anonymous authentication is provided`() {
        // Arrange - set anonymous authentication
        val anonymousAuth = UsernamePasswordAuthenticationToken(
            "anonymousUser", null, listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        )
        SecurityContextHolder.getContext().authentication = anonymousAuth
        
        // Act
        val hasPermResult = client.hasPermission(mockRequest, "channel1", ChannelPermission.READ)
        val channelsResult = client.getChannelsWithPermission(mockRequest, ChannelPermission.READ)
        val permissionsResult = client.getAllChannelPermissions(mockRequest)
        
        // Assert
        assertFalse(hasPermResult)
        assertTrue(channelsResult.isEmpty())
        assertTrue(permissionsResult.isEmpty())
        
        // Verify no REST calls were made
        verifyNoInteractions(restTemplate)
    }
}