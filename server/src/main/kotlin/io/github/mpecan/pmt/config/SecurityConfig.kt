package io.github.mpecan.pmt.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Security configuration for the application.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val pushpinProperties: PushpinProperties) {

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
            }.let { innerHttp ->
                if (pushpinProperties.authEnabled) {
                    innerHttp.addFilterBefore(
                        PushpinAuthFilter(pushpinProperties.authSecret),
                        UsernamePasswordAuthenticationFilter::class.java,
                    ).authorizeHttpRequests {
                        it
                            .anyRequest().authenticated()
                    }
                } else {
                    // If auth is disabled, allow all requests
                    innerHttp.authorizeHttpRequests { authorize ->
                        authorize.anyRequest().permitAll()
                    }
                }
            }

        return http.build()
    }

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
 * Authentication filter for Pushpin requests.
 */
class PushpinAuthFilter(private val authSecret: String) : org.springframework.web.filter.OncePerRequestFilter() {
    override fun doFilterInternal(
        request: jakarta.servlet.http.HttpServletRequest,
        response: jakarta.servlet.http.HttpServletResponse,
        filterChain: jakarta.servlet.FilterChain,
    ) {
        val authHeader = request.getHeader("X-Pushpin-Auth")

        if (authHeader != null && authHeader == authSecret) {
            // Create authentication token
            val auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "pushpin",
                null,
                listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PUSHPIN")),
            )
            org.springframework.security.core.context.SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
