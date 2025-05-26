package io.github.mpecan.pmt.controller

/**
 * Request for granting channel permissions to a role.
 */
data class RolePermissionRequest(
    val role: String,
    val channelId: String,
    val permissions: List<String>
)