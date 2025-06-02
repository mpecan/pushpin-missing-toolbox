package io.github.mpecan.pmt.security.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import io.github.mpecan.pmt.security.core.AuditService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Filter for rate limiting requests.
 *
 * @param properties The rate limiting configuration properties
 * @param auditService The audit service for logging rate limit events
 * @param buckets A map to store rate limit buckets, keyed by IP address or username.
 *                This is injectable to allow for better testability.
 */
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val auditService: AuditService,
    private val buckets: ConcurrentHashMap<String, Bucket> = ConcurrentHashMap(),
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Skip rate limiting if disabled
        if (!properties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        // Get the key for rate limiting (username if authenticated, IP address otherwise)
        val key = getRequestKey(request)

        // Get or create the bucket for this key
        val bucket = buckets.computeIfAbsent(key.toString()) { createBucket() }

        // Try to consume a token from the bucket
        if (bucket.tryConsume(1)) {
            // Request is allowed, proceed with the filter chain
            filterChain.doFilter(request, response)
        } else {
            // Rate limit exceeded, return 429 Too Many Requests
            auditService.logRateLimitExceeded(key.username, key.ip)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("X-Rate-Limit-Exceeded", "true")
            response.contentType = "application/json"
            response.writer.write("{\"error\":\"Rate limit exceeded. Please try again later.\"}")
        }
    }

    /**
     * Get a key for the request for rate limiting purposes.
     * Uses the username if authenticated, or the IP address otherwise.
     */
    private fun getRequestKey(request: HttpServletRequest): RequestKey {
        // Get the authenticated username if available
        val authentication =
            org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .authentication
        val username =
            if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
                authentication.name
            } else {
                null
            }

        // Otherwise, use the client IP address
        return RequestKey(getClientIP(request), username)
    }

    inner class RequestKey(
        val ip: String,
        val username: String?,
    ) {
        override fun toString(): String = if (username != null) "user:$username" else "ip:$ip"
    }

    /**
     * Extract the client IP address from the request.
     */
    private fun getClientIP(request: HttpServletRequest): String {
        // Try common headers for proxied requests
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            val ips = forwardedFor.split(",")
            return ips[0].trim()
        }

        // Try other common headers
        val realIP = request.getHeader("X-Real-IP")
        if (!realIP.isNullOrBlank()) {
            return realIP.trim()
        }

        // Fall back to the remote address
        return request.remoteAddr
    }

    /**
     * Create a new token bucket with the configured capacity and refill rate.
     */
    private fun createBucket(): Bucket {
        val limit = properties.capacity
        val refillTime = properties.refillTimeInMillis

        val refill = Refill.intervally(limit, Duration.ofMillis(refillTime))
        val bandwidth = Bandwidth.classic(limit, refill)

        return Bucket
            .builder()
            .addLimit(bandwidth)
            .build()
    }
}
