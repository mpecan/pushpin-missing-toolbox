package io.github.mpecan.pmt.grip

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GripControlTest {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `should serialize and deserialize GripSubscribeControl`() {
        val control =
            GripSubscribeControl(
                channel = "test-channel",
                filters = listOf("filter1", "filter2"),
                prevId = "prev-123",
            )

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("subscribe", jsonMap["type"])
        assertEquals("test-channel", jsonMap["channel"])
        assertEquals(listOf("filter1", "filter2"), jsonMap["filters"])
        assertEquals("prev-123", jsonMap["prev-id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripSubscribeControl)
        assertEquals("test-channel", deserialized.channel)
        assertEquals(listOf("filter1", "filter2"), deserialized.filters)
        assertEquals("prev-123", deserialized.prevId)
    }

    @Test
    fun `should serialize and deserialize GripSubscribeControl with minimal fields`() {
        val control = GripSubscribeControl(channel = "test-channel")

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("subscribe", jsonMap["type"])
        assertEquals("test-channel", jsonMap["channel"])
        assertNull(jsonMap["filters"])
        assertNull(jsonMap["prev-id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripSubscribeControl)
        assertEquals("test-channel", deserialized.channel)
        assertNull(deserialized.filters)
        assertNull(deserialized.prevId)
    }

    @Test
    fun `should serialize and deserialize GripUnsubscribeControl`() {
        val control = GripUnsubscribeControl(channel = "test-channel")

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("unsubscribe", jsonMap["type"])
        assertEquals("test-channel", jsonMap["channel"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripUnsubscribeControl)
        assertEquals("test-channel", deserialized.channel)
    }

    @Test
    fun `should serialize and deserialize GripDetachControl`() {
        val control = GripDetachControl()

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("detach", jsonMap["type"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripDetachControl)
    }

    @Test
    fun `should serialize and deserialize GripKeepAliveControl`() {
        val control =
            GripKeepAliveControl(
                timeout = 30,
                content = "ping",
                contentBin = "cGluZw==", // "ping" in base64
                format = "json",
            )

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("keep-alive", jsonMap["type"])
        assertEquals(30, jsonMap["timeout"])
        assertEquals("ping", jsonMap["content"])
        assertEquals("cGluZw==", jsonMap["content-bin"])
        assertEquals("json", jsonMap["format"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripKeepAliveControl)
        assertEquals(30, deserialized.timeout)
        assertEquals("ping", deserialized.content)
        assertEquals("cGluZw==", deserialized.contentBin)
        assertEquals("json", deserialized.format)
    }

    @Test
    fun `should serialize and deserialize GripKeepAliveControl with minimal fields`() {
        val control = GripKeepAliveControl()

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("keep-alive", jsonMap["type"])
        assertNull(jsonMap["timeout"])
        assertNull(jsonMap["content"])
        assertNull(jsonMap["content-bin"])
        assertNull(jsonMap["format"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripKeepAliveControl)
        assertNull(deserialized.timeout)
        assertNull(deserialized.content)
        assertNull(deserialized.contentBin)
        assertNull(deserialized.format)
    }

    @Test
    fun `should serialize and deserialize GripSetHoldControl`() {
        val channels =
            listOf(
                GripChannelConfig(
                    name = "channel1",
                    filters = listOf("filter1"),
                    prevId = "prev-1",
                ),
                GripChannelConfig(
                    name = "channel2",
                    filters = listOf("filter2", "filter3"),
                    prevId = "prev-2",
                ),
            )

        val control =
            GripSetHoldControl(
                mode = "stream",
                timeout = 60,
                channels = channels,
            )

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("set-hold", jsonMap["type"])
        assertEquals("stream", jsonMap["mode"])
        assertEquals(60, jsonMap["timeout"])

        val jsonChannels = jsonMap["channels"] as List<*>
        assertEquals(2, jsonChannels.size)

        val channel1 = jsonChannels[0] as Map<*, *>
        assertEquals("channel1", channel1["name"])
        assertEquals(listOf("filter1"), channel1["filters"])
        assertEquals("prev-1", channel1["prev-id"])

        val channel2 = jsonChannels[1] as Map<*, *>
        assertEquals("channel2", channel2["name"])
        assertEquals(listOf("filter2", "filter3"), channel2["filters"])
        assertEquals("prev-2", channel2["prev-id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripSetHoldControl)
        assertEquals("stream", deserialized.mode)
        assertEquals(60, deserialized.timeout)
        assertEquals(2, deserialized.channels?.size)

        val deserializedChannel1 = deserialized.channels!![0]
        assertEquals("channel1", deserializedChannel1.name)
        assertEquals(listOf("filter1"), deserializedChannel1.filters)
        assertEquals("prev-1", deserializedChannel1.prevId)

        val deserializedChannel2 = deserialized.channels[1]
        assertEquals("channel2", deserializedChannel2.name)
        assertEquals(listOf("filter2", "filter3"), deserializedChannel2.filters)
        assertEquals("prev-2", deserializedChannel2.prevId)
    }

    @Test
    fun `should serialize and deserialize GripSetHoldControl with minimal fields`() {
        val control = GripSetHoldControl(mode = "response")

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("set-hold", jsonMap["type"])
        assertEquals("response", jsonMap["mode"])
        assertNull(jsonMap["timeout"])
        assertNull(jsonMap["channels"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripSetHoldControl)
        assertEquals("response", deserialized.mode)
        assertNull(deserialized.timeout)
        assertNull(deserialized.channels)
    }

    @Test
    fun `should serialize and deserialize GripAckControl`() {
        val control =
            GripAckControl(
                channel = "test-channel",
                id = "message-123",
            )

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("ack", jsonMap["type"])
        assertEquals("test-channel", jsonMap["channel"])
        assertEquals("message-123", jsonMap["id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripAckControl)
        assertEquals("test-channel", deserialized.channel)
        assertEquals("message-123", deserialized.id)
    }

    @Test
    fun `should serialize and deserialize GripCloseControl`() {
        val control =
            GripCloseControl(
                code = 1000,
                reason = "Normal closure",
            )

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("close", jsonMap["type"])
        assertEquals(1000, jsonMap["code"])
        assertEquals("Normal closure", jsonMap["reason"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripCloseControl)
        assertEquals(1000, deserialized.code)
        assertEquals("Normal closure", deserialized.reason)
    }

    @Test
    fun `should serialize and deserialize GripCloseControl with minimal fields`() {
        val control = GripCloseControl()

        val json = objectMapper.writeValueAsString(control)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("close", jsonMap["type"])
        assertNull(jsonMap["code"])
        assertNull(jsonMap["reason"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripControl::class.java)
        assertTrue(deserialized is GripCloseControl)
        assertNull(deserialized.code)
        assertNull(deserialized.reason)
    }

    @Test
    fun `should serialize and deserialize GripChannelConfig`() {
        val config =
            GripChannelConfig(
                name = "test-channel",
                filters = listOf("filter1", "filter2"),
                prevId = "prev-123",
            )

        val json = objectMapper.writeValueAsString(config)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("test-channel", jsonMap["name"])
        assertEquals(listOf("filter1", "filter2"), jsonMap["filters"])
        assertEquals("prev-123", jsonMap["prev-id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripChannelConfig::class.java)
        assertEquals("test-channel", deserialized.name)
        assertEquals(listOf("filter1", "filter2"), deserialized.filters)
        assertEquals("prev-123", deserialized.prevId)
    }

    @Test
    fun `should serialize and deserialize GripChannelConfig with minimal fields`() {
        val config = GripChannelConfig(name = "test-channel")

        val json = objectMapper.writeValueAsString(config)

        // Verify JSON structure
        val jsonMap = objectMapper.readValue(json, Map::class.java)
        assertEquals("test-channel", jsonMap["name"])
        assertNull(jsonMap["filters"])
        assertNull(jsonMap["prev-id"])

        // Verify deserialization
        val deserialized = objectMapper.readValue(json, GripChannelConfig::class.java)
        assertEquals("test-channel", deserialized.name)
        assertNull(deserialized.filters)
        assertNull(deserialized.prevId)
    }
}
