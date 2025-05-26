package io.github.mpecan.pmt.security.jwt

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.ReadContext
import io.github.mpecan.pmt.security.core.ClaimExtractorService
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Implementation of ClaimExtractorService that uses JsonPath to extract claims from JWT tokens.
 */
class JsonPathClaimExtractorService : ClaimExtractorService {
    private val logger = LoggerFactory.getLogger(JsonPathClaimExtractorService::class.java)

    override fun extractStringClaim(jwt: Jwt, claimPath: String): String? {
        val context = createJsonContext(jwt)
        return try {
            context.read<Any>(validatePath(claimPath))?.toString()
        } catch (e: PathNotFoundException) {
            logger.debug("Path not found: {}", claimPath)
            null
        } catch (e: Exception) {
            logger.warn("Error extracting claim: {}", e.message)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun extractListClaim(jwt: Jwt, claimPath: String): List<String> {
        val context = createJsonContext(jwt)
        return try {
            val result = context.read<Any>(validatePath(claimPath))
            when (result) {
                is List<*> -> result.filterNotNull().map { it.toString() }
                is String -> listOf(result)
                null -> emptyList()
                else -> listOf(result.toString())
            }
        } catch (e: PathNotFoundException) {
            logger.debug("Path not found: {}", claimPath)
            emptyList()
        } catch (e: Exception) {
            logger.warn("Error extracting list claim: {}", e.message)
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun extractMapClaim(jwt: Jwt, claimPath: String): Map<String, Any> {
        val context = createJsonContext(jwt)
        return try {
            val result = context.read<Any>(validatePath(claimPath))
            when (result) {
                is Map<*, *> -> result.entries
                    .filter { it.key != null }
                    .associate { it.key.toString() to (it.value ?: "") }
                null -> emptyMap()
                else -> emptyMap()
            }
        } catch (e: PathNotFoundException) {
            logger.debug("Path not found: {}", claimPath)
            emptyMap()
        } catch (e: Exception) {
            logger.warn("Error extracting map claim: {}", e.message)
            emptyMap()
        }
    }

    override fun hasClaim(jwt: Jwt, claimPath: String): Boolean {
        val context = createJsonContext(jwt)
        return try {
            context.read<Any>(validatePath(claimPath)) != null
        } catch (e: PathNotFoundException) {
            false
        } catch (e: Exception) {
            logger.warn("Error checking claim existence: {}", e.message)
            false
        }
    }

    /**
     * Creates a JsonPath ReadContext from a JWT token.
     */
    private fun createJsonContext(jwt: Jwt): ReadContext {
        // Combine headers and claims for full access
        val combined = jwt.headers + jwt.claims
        return JsonPath.parse(combined)
    }

    /**
     * Validates and normalizes a JsonPath expression.
     */
    private fun validatePath(path: String): String {
        // Ensure path starts with $
        return if (path.startsWith("$")) {
            path
        } else {
            "$.$path"
        }
    }
}