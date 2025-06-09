package io.github.mpecan.pmt.security.remote

import io.github.mpecan.pmt.security.core.AuditService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Implementation of RemoteAuthorizationClient that uses HTTP to communicate with a remote
 * authorization service for channel subscriptions.
 */
class HttpRemoteSubscriptionClient(
    private val properties: RemoteAuthorizationProperties,
    private val cache: SubscriptionAuthorizationCache,
    private val restTemplate: RestTemplate,
    private val auditService: AuditService,
) : RemoteAuthorizationClient {
    private val logger = LoggerFactory.getLogger(HttpRemoteSubscriptionClient::class.java)

    companion object {
        private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"
    }

    override fun canSubscribe(
        request: HttpServletRequest,
        channelId: String,
    ): Boolean {
        val userId = getCurrentUserId()
        if (userId == null) {
            auditService.logAuthFailure(
                username = "anonymous",
                ipAddress = request.remoteAddr,
                details = "No authenticated user for channel subscription check: $channelId",
            )
            return false
        }

        // Check cache first if enabled
        if (properties.cache.enabled) {
            cache.getSubscriptionCheck(userId, channelId)?.let {
                logger.debug(
                    "Cache hit for subscription check: user={}, channel={}, result={}",
                    userId,
                    channelId,
                    it,
                )
                auditService.logRemoteAuthorizationCheck(
                    username = userId,
                    ipAddress = request.remoteAddr,
                    channelId = channelId,
                    authorized = it,
                    source = "cache",
                )
                return it
            }
        }

        val startTime = System.currentTimeMillis()
        try {
            val result =
                when (properties.method.uppercase()) {
                    "GET" -> checkSubscriptionWithGet(request, userId, channelId)
                    else -> checkSubscriptionWithPost(request, userId, channelId)
                }

            val duration = System.currentTimeMillis() - startTime

            // Cache the result if enabled
            if (properties.cache.enabled) {
                cache.cacheSubscriptionCheck(userId, channelId, result)
            }

            auditService.logRemoteAuthorizationCheck(
                username = userId,
                ipAddress = request.remoteAddr,
                channelId = channelId,
                authorized = result,
                source = "remote",
                duration = duration,
            )

            return result
        } catch (e: Exception) {
            logger.error("Error checking subscription with remote service: {}", e.message)
            auditService.logRemoteAuthorizationError(
                username = userId,
                ipAddress = request.remoteAddr,
                channelId = channelId,
                error = "${e.javaClass.simpleName}: ${e.message ?: UNKNOWN_ERROR_MESSAGE}",
            )
            return false
        }
    }

    override fun getSubscribableChannels(request: HttpServletRequest): List<String> {
        val userId = getCurrentUserId()
        if (userId == null) {
            auditService.logAuthFailure(
                username = "anonymous",
                ipAddress = request.remoteAddr,
                details = "No authenticated user for channel list retrieval",
            )
            return emptyList()
        }

        // Check cache first if enabled
        if (properties.cache.enabled) {
            cache.getSubscribableChannels(userId)?.let {
                logger.debug("Cache hit for subscribable channels: user={}, channels={}", userId, it)
                auditService.logChannelListRetrieval(
                    username = userId,
                    ipAddress = request.remoteAddr,
                    channelCount = it.size,
                    source = "cache",
                )
                return it
            }
        }

        val startTime = System.currentTimeMillis()
        try {
            val result =
                when (properties.method.uppercase()) {
                    "GET" -> getSubscribableChannelsWithGet(request, userId)
                    else -> getSubscribableChannelsWithPost(request, userId)
                }

            val duration = System.currentTimeMillis() - startTime

            // Cache the result if enabled
            if (properties.cache.enabled) {
                cache.cacheSubscribableChannels(userId, result)
            }

            auditService.logChannelListRetrieval(
                username = userId,
                ipAddress = request.remoteAddr,
                channelCount = result.size,
                source = "remote",
                duration = duration,
            )

            return result
        } catch (e: Exception) {
            logger.error("Error getting subscribable channels from remote service: {}", e.message)
            auditService.logRemoteAuthorizationError(
                username = userId,
                ipAddress = request.remoteAddr,
                channelId = null,
                error =
                    "Channel list retrieval failed - " +
                        "${e.javaClass.simpleName}: ${e.message ?: UNKNOWN_ERROR_MESSAGE}",
            )
            return emptyList()
        }
    }

    override fun getSubscribableChannelsByPattern(
        request: HttpServletRequest,
        pattern: String,
    ): List<String> {
        val userId = getCurrentUserId()
        if (userId == null) {
            auditService.logAuthFailure(
                username = "anonymous",
                ipAddress = request.remoteAddr,
                details = "No authenticated user for channel list by pattern: $pattern",
            )
            return emptyList()
        }

        // Check cache first if enabled
        if (properties.cache.enabled) {
            cache.getSubscribableChannelsByPattern(userId, pattern)?.let {
                logger.debug(
                    "Cache hit for channels by pattern: user={}, pattern={}, channels={}",
                    userId,
                    pattern,
                    it,
                )
                auditService.logChannelListRetrieval(
                    username = userId,
                    ipAddress = request.remoteAddr,
                    channelCount = it.size,
                    source = "cache",
                    pattern = pattern,
                )
                return it
            }
        }

        val startTime = System.currentTimeMillis()
        try {
            val result =
                when (properties.method.uppercase()) {
                    "GET" -> getChannelsByPatternWithGet(request, userId, pattern)
                    else -> getChannelsByPatternWithPost(request, userId, pattern)
                }

            val duration = System.currentTimeMillis() - startTime

            // Cache the result if enabled
            if (properties.cache.enabled) {
                cache.cacheSubscribableChannelsByPattern(userId, pattern, result)
            }

            auditService.logChannelListRetrieval(
                username = userId,
                ipAddress = request.remoteAddr,
                channelCount = result.size,
                source = "remote",
                duration = duration,
                pattern = pattern,
            )

            return result
        } catch (e: Exception) {
            logger.error("Error getting channels by pattern from remote service: {}", e.message)
            auditService.logRemoteAuthorizationError(
                username = userId,
                ipAddress = request.remoteAddr,
                channelId = null,
                error =
                    "Channel list by pattern failed - ${e.javaClass.simpleName}: " +
                        "${e.message ?: UNKNOWN_ERROR_MESSAGE} (pattern: $pattern)",
            )
            return emptyList()
        }
    }

    /**
     * Check subscription using HTTP GET.
     */
    private fun checkSubscriptionWithGet(
        request: HttpServletRequest,
        userId: String,
        channelId: String,
    ): Boolean {
        val uri =
            UriComponentsBuilder
                .fromUriString(properties.url)
                .path("/subscribe/check")
                .queryParam("userId", userId)
                .queryParam("channelId", channelId)
                .build()
                .toUri()

        val headers = createHeaders(request)
        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                SubscriptionResponse::class.java,
            )

        return response.body?.allowed ?: false
    }

    /**
     * Check subscription using HTTP POST.
     */
    private fun checkSubscriptionWithPost(
        request: HttpServletRequest,
        userId: String,
        channelId: String,
    ): Boolean {
        val uri = URI.create("${properties.url}/subscribe/check")

        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON

        val body =
            mapOf(
                "userId" to userId,
                "channelId" to channelId,
            )

        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.POST,
                HttpEntity(body, headers),
                SubscriptionResponse::class.java,
            )

        return response.body?.allowed ?: false
    }

    /**
     * Get subscribable channels using HTTP GET.
     */
    private fun getSubscribableChannelsWithGet(
        request: HttpServletRequest,
        userId: String,
    ): List<String> {
        val uri =
            UriComponentsBuilder
                .fromUriString(properties.url)
                .path("/subscribe/channels")
                .queryParam("userId", userId)
                .build()
                .toUri()

        val headers = createHeaders(request)
        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                ChannelsResponse::class.java,
            )

        return response.body?.channels ?: emptyList()
    }

    /**
     * Get subscribable channels using HTTP POST.
     */
    private fun getSubscribableChannelsWithPost(
        request: HttpServletRequest,
        userId: String,
    ): List<String> {
        val uri = URI.create("${properties.url}/subscribe/channels")

        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON

        val body = mapOf("userId" to userId)

        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.POST,
                HttpEntity(body, headers),
                ChannelsResponse::class.java,
            )

        return response.body?.channels ?: emptyList()
    }

    /**
     * Get channels by pattern using HTTP GET.
     */
    private fun getChannelsByPatternWithGet(
        request: HttpServletRequest,
        userId: String,
        pattern: String,
    ): List<String> {
        val uri =
            UriComponentsBuilder
                .fromUriString(properties.url)
                .path("/subscribe/channels/pattern")
                .queryParam("userId", userId)
                .queryParam("pattern", pattern)
                .build()
                .toUri()

        val headers = createHeaders(request)
        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                ChannelsResponse::class.java,
            )

        return response.body?.channels ?: emptyList()
    }

    /**
     * Get channels by pattern using HTTP POST.
     */
    private fun getChannelsByPatternWithPost(
        request: HttpServletRequest,
        userId: String,
        pattern: String,
    ): List<String> {
        val uri = URI.create("${properties.url}/subscribe/channels/pattern")

        val headers = createHeaders(request)
        headers.contentType = MediaType.APPLICATION_JSON

        val body =
            mapOf(
                "userId" to userId,
                "pattern" to pattern,
            )

        val response =
            restTemplate.exchange(
                uri,
                HttpMethod.POST,
                HttpEntity(body, headers),
                ChannelsResponse::class.java,
            )

        return response.body?.channels ?: emptyList()
    }

    /**
     * Create HTTP headers for the request.
     */
    private fun createHeaders(request: HttpServletRequest): HttpHeaders {
        val headers = HttpHeaders()

        // Include configured headers from the original request
        for (headerName in properties.includeHeaders) {
            val headerValue = request.getHeader(headerName)
            if (headerValue != null) {
                headers.add(headerName, headerValue)
            }
        }

        return headers
    }

    /**
     * Get the current user ID from the security context.
     */
    private fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            authentication.name
        } else {
            null
        }
    }

    /**
     * Response from the authorization service for subscription checks.
     */
    internal data class SubscriptionResponse(
        val allowed: Boolean,
        val metadata: Map<String, Any>? = null,
    )

    /**
     * Response from the authorization service for channel lists.
     */
    internal data class ChannelsResponse(
        val channels: List<String>,
    )
}
