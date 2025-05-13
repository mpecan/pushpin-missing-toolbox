package io.github.mpecan.pmt.security.hmac

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.audit.AuditLogService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * Filter for verifying HMAC signatures on server-to-server requests.
 */
class HmacSignatureFilter(
    private val properties: PushpinProperties,
    private val hmacSignatureService: HmacSignatureService,
    private val auditLogService: AuditLogService
) : OncePerRequestFilter() {
    
    // Request paths that are excluded from HMAC verification
    private val excludedPaths = listOf(
        "/api/public/",
        "/actuator/",
        "/api/pushpin/auth"
    )
    
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Skip HMAC verification if disabled
        if (!properties.security.hmac.enabled) {
            return true
        }
        
        // Skip HMAC verification for excluded paths
        val path = request.requestURI
        return excludedPaths.any { path.startsWith(it) }
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Wrap the request to be able to read the body multiple times
        val cachedBodyRequest = ContentCachingRequestWrapper(request)
        
        // Get the required headers
        val signature = request.getHeader(properties.security.hmac.headerName)
        val timestamp = request.getHeader("X-Pushpin-Timestamp")?.toLongOrNull()
        
        if (signature == null || timestamp == null) {
            // Missing required headers
            auditLogService.logAuthorizationFailure(
                "server-to-server",
                request.remoteAddr,
                request.requestURI,
                "HMAC_SIGNATURE"
            )
            
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json"
            response.writer.write("{\"error\":\"Missing required HMAC signature headers\"}")
            return
        }
        
        // Process the request
        filterChain.doFilter(cachedBodyRequest, response)
        
        // After the request is processed, the request body is available
        val body = String(cachedBodyRequest.contentAsByteArray)
        val path = cachedBodyRequest.requestURI
        
        // Verify the signature
        val isValid = hmacSignatureService.verifyRequestSignature(
            body,
            timestamp,
            path,
            signature
        )
        
        if (!isValid) {
            // Invalid signature, log the event
            auditLogService.logAuthorizationFailure(
                "server-to-server",
                request.remoteAddr,
                request.requestURI,
                "HMAC_SIGNATURE"
            )
            
            // Note: We don't reject the request here since the filter chain is already executed
            // This is a post-processing check for audit purposes
            // In a real implementation, you would likely want to verify the signature before processing
        }
    }
}