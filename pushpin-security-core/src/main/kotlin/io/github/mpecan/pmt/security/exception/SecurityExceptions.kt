package io.github.mpecan.pmt.security.exception

/**
 * Base exception for all security-related errors in the Pushpin security framework.
 */
open class PushpinSecurityException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when authorization fails.
 */
class AuthorizationException(
    message: String,
    cause: Throwable? = null
) : PushpinSecurityException(message, cause)

/**
 * Exception thrown when authentication fails.
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : PushpinSecurityException(message, cause)

/**
 * Exception thrown when there is a configuration error in the security framework.
 */
class SecurityConfigurationException(
    message: String,
    cause: Throwable? = null
) : PushpinSecurityException(message, cause)