package io.github.mpecan.pmt.security.model

/**
 * Represents a permission level for a channel.
 */
enum class ChannelPermission {
    /**
     * Read-only access to the channel.
     */
    READ,
    
    /**
     * Ability to publish messages to the channel.
     */
    WRITE,
    
    /**
     * Full administrative access to the channel.
     */
    ADMIN
}