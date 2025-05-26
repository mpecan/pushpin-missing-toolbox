package io.github.mpecan.pmt.controller

/**
 * Request for granting channel permissions to a user.
 */
data class PermissionRequest(
    val username: String,
    val channelId: String,
    val permissions: List<String>
)