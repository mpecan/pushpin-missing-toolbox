package io.github.mpecan.pmt.security.audit

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for audit logging.
 */
@ConfigurationProperties(prefix = "pushpin.security.audit")
data class AuditProperties(
    /**
     * Whether audit logging is enabled.
     */
    val enabled: Boolean = false,

    /**
     * Audit logging level (DEBUG, INFO, WARN, ERROR).
     */
    val level: String = "INFO",

    /**
     * Whether to include stack traces in audit logs.
     */
    val includeStackTrace: Boolean = false,

    /**
     * Whether to log successful authentication events.
     */
    val logSuccessfulAuth: Boolean = true,

    /**
     * Whether to log failed authentication events.
     */
    val logFailedAuth: Boolean = true,

    /**
     * Whether to log channel access events.
     */
    val logChannelAccess: Boolean = true,

    /**
     * Whether to log administrative actions.
     */
    val logAdminActions: Boolean = true,

    /**
     * Maximum length of audit log messages before truncation.
     */
    val maxMessageLength: Int = 1000,

    /**
     * Whether to include request headers in audit logs.
     */
    val includeRequestHeaders: Boolean = false,

    /**
     * List of sensitive headers to exclude from logs.
     */
    val excludeHeaders: List<String> = listOf("Authorization", "Cookie", "X-Auth-Token"),
)
