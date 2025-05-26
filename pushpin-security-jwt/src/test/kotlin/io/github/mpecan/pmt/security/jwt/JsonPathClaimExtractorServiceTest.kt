package io.github.mpecan.pmt.security.jwt

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.*

class JsonPathClaimExtractorServiceTest {

    private val extractor = JsonPathClaimExtractorService()

    private fun createTestJwt(claims: Map<String, Any>): Jwt {
        return Jwt.withTokenValue("test-token")
            .headers { it.put("typ", "JWT") }
            .claims { it.putAll(claims) }
            .build()
    }

    @Test
    fun `should extract string claim`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        val result = extractor.extractStringClaim(jwt, "user")

        assertEquals("john.doe", result)
    }

    @Test
    fun `should extract string claim with JsonPath`() {
        val jwt = createTestJwt(mapOf("nested" to mapOf("user" to "jane.doe")))

        val result = extractor.extractStringClaim(jwt, "$.nested.user")

        assertEquals("jane.doe", result)
    }

    @Test
    fun `should return null for non-existent claim`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        val result = extractor.extractStringClaim(jwt, "nonexistent")

        assertNull(result)
    }

    @Test
    fun `should extract list claim from array`() {
        val jwt = createTestJwt(mapOf("roles" to listOf("admin", "user")))

        val result = extractor.extractListClaim(jwt, "roles")

        assertEquals(listOf("admin", "user"), result)
    }

    @Test
    fun `should extract list claim from string`() {
        val jwt = createTestJwt(mapOf("role" to "admin"))

        val result = extractor.extractListClaim(jwt, "role")

        assertEquals(listOf("admin"), result)
    }

    @Test
    fun `should return empty list for non-existent list claim`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        val result = extractor.extractListClaim(jwt, "roles")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `should extract map claim`() {
        val permissions = mapOf("read" to "true", "write" to "false")
        val jwt = createTestJwt(mapOf("permissions" to permissions))

        val result = extractor.extractMapClaim(jwt, "permissions")

        assertEquals(mapOf("read" to "true", "write" to "false"), result)
    }

    @Test
    fun `should return empty map for non-existent map claim`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        val result = extractor.extractMapClaim(jwt, "permissions")

        assertEquals(emptyMap(), result)
    }

    @Test
    fun `should check if claim exists`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        assertTrue(extractor.hasClaim(jwt, "user"))
        assertFalse(extractor.hasClaim(jwt, "nonexistent"))
    }

    @Test
    fun `should validate JsonPath expressions`() {
        val jwt = createTestJwt(mapOf("user" to "john.doe"))

        // Both should work the same way
        val result1 = extractor.extractStringClaim(jwt, "user")
        val result2 = extractor.extractStringClaim(jwt, "$.user")

        assertEquals(result1, result2)
    }
}
