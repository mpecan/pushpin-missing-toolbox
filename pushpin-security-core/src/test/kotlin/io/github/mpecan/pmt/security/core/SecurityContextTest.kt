package io.github.mpecan.pmt.security.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SecurityContextTest {
    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should store and retrieve security context`() {
        val context =
            SecurityContext(
                principal = "test-user",
                authenticated = true,
                attributes = mapOf("role" to "ADMIN"),
            )

        SecurityContextHolder.setContext(context)
        val retrieved = SecurityContextHolder.getContext()

        assertThat(retrieved).isEqualTo(context)
        assertThat(retrieved.principal).isEqualTo("test-user")
        assertThat(retrieved.authenticated).isTrue()
        assertThat(retrieved.getAttribute<String>("role")).isEqualTo("ADMIN")
    }

    @Test
    fun `should return empty context when not set`() {
        val context = SecurityContextHolder.getContext()

        assertThat(context.principal).isNull()
        assertThat(context.authenticated).isFalse()
        assertThat(context.attributes).isEmpty()
    }

    @Test
    fun `should clear context`() {
        val context = SecurityContext(principal = "test-user", authenticated = true)
        SecurityContextHolder.setContext(context)

        SecurityContextHolder.clearContext()

        val retrieved = SecurityContextHolder.getContext()
        assertThat(retrieved.principal).isNull()
        assertThat(retrieved.authenticated).isFalse()
    }

    @Test
    fun `should add attributes to context`() {
        val context =
            SecurityContext(principal = "test-user")
                .withAttribute("role", "USER")
                .withAttribute("tenant", "test-tenant")

        assertThat(context.getAttribute<String>("role")).isEqualTo("USER")
        assertThat(context.getAttribute<String>("tenant")).isEqualTo("test-tenant")
    }

    @Test
    fun `should add multiple attributes to context`() {
        val context =
            SecurityContext(principal = "test-user")
                .withAttributes(
                    mapOf(
                        "role" to "USER",
                        "tenant" to "test-tenant",
                        "permissions" to listOf("READ", "WRITE"),
                    ),
                )

        assertThat(context.getAttribute<String>("role")).isEqualTo("USER")
        assertThat(context.getAttribute<String>("tenant")).isEqualTo("test-tenant")
        assertThat(context.getAttribute<List<String>>("permissions"))
            .containsExactly("READ", "WRITE")
    }

    @Test
    fun `should handle thread-local isolation`() {
        val context1 = SecurityContext(principal = "user1")
        val context2 = SecurityContext(principal = "user2")

        SecurityContextHolder.setContext(context1)

        // In a different thread
        val thread =
            Thread {
                SecurityContextHolder.setContext(context2)
                assertThat(SecurityContextHolder.getContext().principal).isEqualTo("user2")
            }
        thread.start()
        thread.join()

        // Original thread should still have context1
        assertThat(SecurityContextHolder.getContext().principal).isEqualTo("user1")
    }
}
