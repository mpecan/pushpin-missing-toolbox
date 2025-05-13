package io.github.mpecan.pmt.config

import io.github.mpecan.pmt.security.JwtDecoderProvider
import io.github.mpecan.pmt.security.RateLimitFilter
import io.github.mpecan.pmt.security.audit.AuditLogService
import io.github.mpecan.pmt.security.hmac.HmacSignatureFilter
import io.github.mpecan.pmt.security.hmac.HmacSignatureService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Security configuration for the application.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val pushpinProperties: PushpinProperties,
    private val jwtDecoderProvider: JwtDecoderProvider,
    private val jwtAuthenticationConverter: Converter<Jwt, AbstractAuthenticationToken>,
    private val hmacSignatureService: HmacSignatureService,
    private val auditLogService: AuditLogService
) {
    /**
     * Configures the security filter chain.
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/pushpin/auth").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
            }

        // Add rate limiting if enabled
        if (pushpinProperties.security.rateLimit.enabled) {
            http.addFilterBefore(
                RateLimitFilter(pushpinProperties, auditLogService),
                UsernamePasswordAuthenticationFilter::class.java
            )
        }

        // Add HMAC signature verification if enabled
        if (pushpinProperties.security.hmac.enabled) {
            http.addFilterBefore(
                HmacSignatureFilter(pushpinProperties, hmacSignatureService, auditLogService),
                UsernamePasswordAuthenticationFilter::class.java
            )
        }

        // Add appropriate authentication method
        if (pushpinProperties.authEnabled) {
            if (pushpinProperties.security.jwt.enabled) {
                // OAuth2 Resource Server with JWT
                http.oauth2ResourceServer { oauth2 ->
                    oauth2.jwt { jwt ->
                        jwt.decoder(jwtDecoderProvider.getDecoder())
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                    }
                }
            } else {
                // Legacy token-based authentication
                http.addFilterBefore(
                    PushpinAuthFilter(pushpinProperties.authSecret, auditLogService),
                    UsernamePasswordAuthenticationFilter::class.java
                )
            }

            // Require authentication for all remaining endpoints
            http.authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }
        } else {
            // If auth is disabled, allow all requests
            http.authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }
        }

        return http.build()
    }

    /**
     * Password encoder for secure password storage.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * Configures CORS.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}

/**
 * Authentication filter for Pushpin requests using the legacy token-based approach.
 */
class PushpinAuthFilter(
    private val authSecret: String,
    private val auditLogService: AuditLogService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("X-Pushpin-Auth")

        if (authHeader != null && authHeader == authSecret) {
            // Create authentication token
            val auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "pushpin", null, listOf(SimpleGrantedAuthority("ROLE_PUSHPIN"))
            )
            SecurityContextHolder.getContext().authentication = auth

            // Log successful authentication
            auditLogService.logAuthSuccess(
                "pushpin",
                request.remoteAddr,
                "Legacy token authentication"
            )
        } else if (authHeader != null) {
            // Log failed authentication
            auditLogService.logAuthFailure(
                "unknown",
                request.remoteAddr,
                "Invalid legacy token"
            )
        }

        filterChain.doFilter(request, response)
    }
}