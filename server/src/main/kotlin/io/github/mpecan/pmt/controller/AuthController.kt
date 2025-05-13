package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.model.ChannelPermission
import io.github.mpecan.pmt.security.service.ChannelAuthorizationService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.crypto.spec.SecretKeySpec

/**
 * Controller for authentication and authorization.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val pushpinProperties: PushpinProperties,
    private val channelAuthorizationService: ChannelAuthorizationService,
    private val auditLogService: AuditLogService
) {

    /**
     * Generates a JWT token for the given credentials.
     * Note: This endpoint is only for development/testing with the symmetric key provider.
     * In production, you should use a proper OAuth2 authorization server.
     */
    @PostMapping("/token")
    fun getToken(@RequestBody request: AuthRequest, servletRequest: HttpServletRequest): ResponseEntity<Map<String, String>> {
        // In a real system, you would use a proper OAuth2 authorization server
        // This is just for development/testing with the symmetric key provider
        if (pushpinProperties.security.jwt.provider != "symmetric") {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "This endpoint is only available when using the symmetric JWT provider"
            ))
        }

        // Generate a JWT token
        val now = Date()
        val expiration = Date(now.time + pushpinProperties.security.jwt.expirationMs)
        val roles = listOf("ROLE_USER")

        val secretKey = SecretKeySpec(
            pushpinProperties.security.jwt.secret.toByteArray(),
            "HMAC"
        )

        val token = Jwts.builder()
            .setSubject(request.username)
            .claim(pushpinProperties.security.jwt.authoritiesClaim, roles)
            .setIssuer(pushpinProperties.security.jwt.issuer)
            .setAudience(pushpinProperties.security.jwt.audience)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()

        // Log the authentication event
        auditLogService.logAuthSuccess(
            request.username,
            servletRequest.remoteAddr,
            "JWT token generated"
        )

        return ResponseEntity.ok(mapOf("token" to token))
    }
    
    /**
     * Grants channel permissions to a user.
     */
    @PostMapping("/permissions/user")
    fun grantUserPermissions(
        @RequestBody request: PermissionRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<Map<String, Boolean>> {
        val permissions = request.permissions.map { ChannelPermission.valueOf(it.uppercase()) }
        
        channelAuthorizationService.grantUserPermissions(
            request.username,
            request.channelId,
            *permissions.toTypedArray()
        )
        
        // Log the permission change
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        val adminUsername = authentication?.name ?: "system"
        
        auditLogService.logSecurityConfigChange(
            adminUsername,
            servletRequest.remoteAddr,
            "Granted permissions ${permissions.joinToString(", ")} to user ${request.username} for channel ${request.channelId}"
        )
        
        return ResponseEntity.ok(mapOf("success" to true))
    }
    
    /**
     * Grants channel permissions to a role.
     */
    @PostMapping("/permissions/role")
    fun grantRolePermissions(
        @RequestBody request: RolePermissionRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<Map<String, Boolean>> {
        val permissions = request.permissions.map { ChannelPermission.valueOf(it.uppercase()) }
        
        channelAuthorizationService.grantRolePermissions(
            request.role,
            request.channelId,
            *permissions.toTypedArray()
        )
        
        // Log the permission change
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        val adminUsername = authentication?.name ?: "system"
        
        auditLogService.logSecurityConfigChange(
            adminUsername,
            servletRequest.remoteAddr,
            "Granted permissions ${permissions.joinToString(", ")} to role ${request.role} for channel ${request.channelId}"
        )
        
        return ResponseEntity.ok(mapOf("success" to true))
    }
    
    /**
     * Gets all channels the current user has access to with the given permission.
     */
    @GetMapping("/permissions/channels")
    fun getChannelsWithPermission(
        @RequestParam permission: String,
        servletRequest: HttpServletRequest
    ): ResponseEntity<List<String>> {
        val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
            ?: return ResponseEntity.ok(emptyList())
        
        val channelPermission = ChannelPermission.valueOf(permission.uppercase())
        
        // Get all channels the user has access to
        val allChannels = channelAuthorizationService.getChannelsWithPermission(
            authentication,
            channelPermission
        )
        
        return ResponseEntity.ok(allChannels)
    }
}

/**
 * Request for generating a JWT token.
 */
data class AuthRequest(
    val username: String,
    val password: String
)

/**
 * Request for granting channel permissions to a user.
 */
data class PermissionRequest(
    val username: String,
    val channelId: String,
    val permissions: List<String>
)

/**
 * Request for granting channel permissions to a role.
 */
data class RolePermissionRequest(
    val role: String,
    val channelId: String,
    val permissions: List<String>
)