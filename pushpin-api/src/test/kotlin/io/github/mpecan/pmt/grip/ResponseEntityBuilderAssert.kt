package io.github.mpecan.pmt.grip

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Fluent assertion helper for testing ResponseEntity.BodyBuilder instances.
 * This class wraps a builder and provides easy-to-use assertion methods.
 *
 * Usage example:
 * ```kotlin
 * val builder = GripApi.longPollingResponse("test-channel", 30)
 *
 * assertThat(builder)
 *     .hasStatus(HttpStatus.OK)
 *     .hasContentType(MediaType.APPLICATION_JSON)
 *     .hasHeader(GripConstants.HEADER_GRIP_HOLD, GripConstants.HOLD_MODE_RESPONSE)
 *     .hasHeader(GripConstants.HEADER_GRIP_CHANNEL, "test-channel")
 *     .hasHeader(GripConstants.HEADER_GRIP_TIMEOUT, "30")
 * ```
 */
@Suppress("unused")
class ResponseEntityBuilderAssert(
    private val builder: ResponseEntity.BodyBuilder,
) {
    private val builtEntity: ResponseEntity<*> by lazy {
        builder.build<Any>()
    }

    /**
     * Asserts that the builder has the expected status.
     */
    fun hasStatus(expectedStatus: HttpStatus): ResponseEntityBuilderAssert {
        assertEquals(
            expectedStatus,
            builtEntity.statusCode,
            "Expected status $expectedStatus but was ${builtEntity.statusCode}",
        )
        return this
    }

    /**
     * Asserts that the builder has the expected content type.
     */
    fun hasContentType(expectedContentType: MediaType): ResponseEntityBuilderAssert {
        val actualContentType = builtEntity.headers.contentType
        assertNotNull(actualContentType, "Expected content type $expectedContentType but was null")
        assertEquals(
            expectedContentType,
            actualContentType,
            "Expected content type $expectedContentType but was $actualContentType",
        )
        return this
    }

    /**
     * Asserts that the builder has a specific header with the expected value.
     */
    fun hasHeader(
        headerName: String,
        expectedValue: String,
    ): ResponseEntityBuilderAssert {
        val actualValue = builtEntity.headers.getFirst(headerName)
        assertNotNull(
            actualValue,
            "Expected header '$headerName' with value '$expectedValue' but header was not present",
        )
        assertEquals(
            expectedValue,
            actualValue,
            "Expected header '$headerName' to have value '$expectedValue' but was '$actualValue'",
        )
        return this
    }

    /**
     * Asserts that the builder has a specific header (regardless of value).
     */
    fun hasHeader(headerName: String): ResponseEntityBuilderAssert {
        assertTrue(
            builtEntity.headers.containsKey(headerName),
            "Expected header '$headerName' to be present but it was not",
        )
        return this
    }

    /**
     * Asserts that the builder does not have a specific header.
     */
    fun doesNotHaveHeader(headerName: String): ResponseEntityBuilderAssert {
        assertTrue(
            !builtEntity.headers.containsKey(headerName),
            "Expected header '$headerName' to not be present but it was",
        )
        return this
    }

    /**
     * Asserts that the builder has all the expected headers with their values.
     */
    fun hasHeaders(vararg headers: Pair<String, String>): ResponseEntityBuilderAssert {
        headers.forEach { (name, value) ->
            hasHeader(name, value)
        }
        return this
    }

    /**
     * Asserts the exact number of headers present.
     */
    fun hasHeaderCount(expectedCount: Int): ResponseEntityBuilderAssert {
        val actualCount = builtEntity.headers.size
        assertEquals(expectedCount, actualCount, "Expected $expectedCount headers but found $actualCount")
        return this
    }

    /**
     * Provides access to the headers for custom assertions.
     */
    fun withHeaders(assertion: (HttpHeaders) -> Unit): ResponseEntityBuilderAssert {
        assertion(builtEntity.headers)
        return this
    }

    /**
     * Asserts that the builder is a successful response (2xx status).
     */
    fun isSuccessful(): ResponseEntityBuilderAssert {
        assertTrue(
            builtEntity.statusCode.is2xxSuccessful,
            "Expected successful response (2xx) but was ${builtEntity.statusCode}",
        )
        return this
    }

    companion object {
        /**
         * Creates a new assertion instance for the given builder.
         */
        fun assertThat(builder: ResponseEntity.BodyBuilder): ResponseEntityBuilderAssert =
            ResponseEntityBuilderAssert(builder)
    }
}

/**
 * Extension function to make assertions even more fluent.
 */
fun assertThat(builder: ResponseEntity.BodyBuilder): ResponseEntityBuilderAssert =
    ResponseEntityBuilderAssert.assertThat(builder)
