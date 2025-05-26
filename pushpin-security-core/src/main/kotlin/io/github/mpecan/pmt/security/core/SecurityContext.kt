package io.github.mpecan.pmt.security.core

/**
 * Represents the security context for the current request or operation.
 * This can be used to store authentication and authorization information.
 */
data class SecurityContext(
    val principal: Any? = null,
    val authenticated: Boolean = false,
    val attributes: Map<String, Any> = emptyMap(),
) {
    /**
     * Get an attribute from the security context.
     */
    inline fun <reified T> getAttribute(key: String): T? {
        return attributes[key] as? T
    }

    /**
     * Create a new SecurityContext with an additional attribute.
     */
    fun withAttribute(key: String, value: Any): SecurityContext {
        return copy(attributes = attributes + (key to value))
    }

    /**
     * Create a new SecurityContext with multiple additional attributes.
     */
    fun withAttributes(newAttributes: Map<String, Any>): SecurityContext {
        return copy(attributes = attributes + newAttributes)
    }
}

/**
 * A holder for the security context that can be used across the application.
 * This follows a similar pattern to Spring Security's SecurityContextHolder.
 */
object SecurityContextHolder {
    private val contextHolder = ThreadLocal<SecurityContext>()

    /**
     * Get the current security context.
     */
    fun getContext(): SecurityContext {
        return contextHolder.get() ?: SecurityContext()
    }

    /**
     * Set the security context.
     */
    fun setContext(context: SecurityContext) {
        contextHolder.set(context)
    }

    /**
     * Clear the security context.
     */
    fun clearContext() {
        contextHolder.remove()
    }
}
