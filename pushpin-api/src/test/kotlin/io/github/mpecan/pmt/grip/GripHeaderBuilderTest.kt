package io.github.mpecan.pmt.grip

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GripHeaderBuilderTest {
    @Test
    fun `should build headers with hold mode`() {
        val headers =
            GripHeaderBuilder()
                .holdResponse()
                .build()

        assertEquals(GripConstants.HOLD_MODE_RESPONSE, headers[GripConstants.HEADER_GRIP_HOLD])
    }

    @Test
    fun `should build headers with stream mode`() {
        val headers =
            GripHeaderBuilder()
                .holdStream()
                .build()

        assertEquals(GripConstants.HOLD_MODE_STREAM, headers[GripConstants.HEADER_GRIP_HOLD])
    }

    @Test
    fun `should build headers with single channel`() {
        val headers =
            GripHeaderBuilder()
                .channel("test-channel")
                .build()

        assertEquals("test-channel", headers[GripConstants.HEADER_GRIP_CHANNEL])
    }

    @Test
    fun `should build headers with multiple channels`() {
        val headers =
            GripHeaderBuilder()
                .channels("channel1", "channel2", "channel3")
                .build()

        assertEquals("channel1, channel2, channel3", headers[GripConstants.HEADER_GRIP_CHANNEL])
    }

    @Test
    fun `should build headers with timeout`() {
        val headers =
            GripHeaderBuilder()
                .timeout(30)
                .build()

        assertEquals("30", headers[GripConstants.HEADER_GRIP_TIMEOUT])
    }

    @Test
    fun `should build headers with keep-alive settings`() {
        val headers =
            GripHeaderBuilder()
                .keepAlive("ping")
                .keepAliveFormat("json")
                .keepAliveTimeout(20)
                .build()

        assertEquals("ping", headers[GripConstants.HEADER_GRIP_KEEP_ALIVE])
        assertEquals("json", headers[GripConstants.HEADER_GRIP_KEEP_ALIVE_FORMAT])
        assertEquals("20", headers[GripConstants.HEADER_GRIP_KEEP_ALIVE_TIMEOUT])
    }

    @Test
    fun `should build headers with GRIP signature`() {
        val headers =
            GripHeaderBuilder()
                .gripSig("test-signature")
                .build()

        assertEquals("test-signature", headers[GripConstants.HEADER_GRIP_SIG])
    }

    @Test
    fun `should build headers with custom headers`() {
        val headers =
            GripHeaderBuilder()
                .header("X-Custom", "value")
                .header("X-Another", "another-value")
                .build()

        assertEquals("value", headers["X-Custom"])
        assertEquals("another-value", headers["X-Another"])
    }

    @Test
    fun `should build complete headers`() {
        val headers =
            GripHeaderBuilder()
                .holdResponse()
                .channel("notifications")
                .timeout(60)
                .gripSig("jwt-token")
                .lastEventId("12345")
                .previousId("12344")
                .build()

        assertEquals(6, headers.size)
        assertEquals(GripConstants.HOLD_MODE_RESPONSE, headers[GripConstants.HEADER_GRIP_HOLD])
        assertEquals("notifications", headers[GripConstants.HEADER_GRIP_CHANNEL])
        assertEquals("60", headers[GripConstants.HEADER_GRIP_TIMEOUT])
        assertEquals("jwt-token", headers[GripConstants.HEADER_GRIP_SIG])
        assertEquals("12345", headers[GripConstants.HEADER_GRIP_LAST])
        assertEquals("12344", headers[GripConstants.HEADER_GRIP_PREVIOUS_ID])
    }
}
