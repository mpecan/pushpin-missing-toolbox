package io.github.mpecan.pmt.controller

/**
 * Request for generating a JWT token.
 */
data class AuthRequest(
    val username: String,
    val password: String
)