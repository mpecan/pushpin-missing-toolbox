package io.github.mpecan.pmt.security.encryption

/**
 * Exception thrown when there is an error during encryption or decryption.
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
