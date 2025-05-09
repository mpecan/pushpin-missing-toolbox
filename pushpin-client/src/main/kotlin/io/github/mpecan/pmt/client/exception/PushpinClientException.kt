package io.github.mpecan.pmt.client.exception

/**
 * Base exception class for Pushpin client errors.
 */
open class PushpinClientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception thrown when there's an error during message serialization.
 */
class MessageSerializationException(
    message: String,
    cause: Throwable? = null,
) : PushpinClientException(message, cause)

/**
 * Exception thrown when there's an error during message formatting.
 */
class MessageFormattingException(
    message: String,
    cause: Throwable? = null,
) : PushpinClientException(message, cause)

/**
 * Exception thrown when there's an error publishing a message to Pushpin.
 */
class PublishingException(
    message: String,
    serverInfo: String? = null,
    statusCode: Int? = null,
    cause: Throwable? = null,
) : PushpinClientException(
    message = when {
        serverInfo != null && statusCode != null -> "$message (Server: $serverInfo, Status: $statusCode)"
        serverInfo != null -> "$message (Server: $serverInfo)"
        statusCode != null -> "$message (Status: $statusCode)"
        else -> message
    },
    cause = cause,
)

/**
 * Exception thrown when no Pushpin servers are available.
 */
class NoServerAvailableException(
    message: String = "No Pushpin servers available",
    cause: Throwable? = null,
) : PushpinClientException(message, cause)

/**
 * Exception thrown when a connection to Pushpin times out.
 */
class PushpinTimeoutException(
    message: String = "Connection to Pushpin server timed out",
    serverInfo: String? = null,
    timeoutMs: Long? = null,
    cause: Throwable? = null,
) : PushpinClientException(
    message = when {
        serverInfo != null && timeoutMs != null -> "$message (Server: $serverInfo, Timeout: ${timeoutMs}ms)"
        serverInfo != null -> "$message (Server: $serverInfo)"
        timeoutMs != null -> "$message (Timeout: ${timeoutMs}ms)"
        else -> message
    },
    cause = cause,
)

/**
 * Exception thrown when there's an authentication error with Pushpin.
 */
class PushpinAuthenticationException(
    message: String = "Authentication with Pushpin server failed",
    serverInfo: String? = null,
    cause: Throwable? = null,
) : PushpinClientException(
    message = if (serverInfo != null) "$message (Server: $serverInfo)" else message,
    cause = cause,
)
