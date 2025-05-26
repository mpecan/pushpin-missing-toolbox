package io.github.mpecan.pmt.grip.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketMessageBuilderTest {
    
    private val objectMapper = ObjectMapper()
    
    @Test
    fun `should build OPEN event`() {
        val message = WebSocketMessageBuilder()
            .open()
            .build()
        
        assertEquals("OPEN\r\n", message)
    }
    
    @Test
    fun `should build TEXT event`() {
        val content = "Hello World"
        val message = WebSocketMessageBuilder()
            .text(content)
            .build()
        
        val expected = "TEXT b\r\nHello World\r\n"
        assertEquals(expected, message)
    }
    
    @Test
    fun `should build message with prefix`() {
        val data = mapOf("status" to "ok", "count" to 42)
        val message = WebSocketMessageBuilder()
            .message(data)
            .build()
        
        // Parse the result to verify structure
        val events = WebSocketEventParser.parse(message)
        assertEquals(1, events.size)
        assertEquals(WebSocketEventType.TEXT, events[0].type)
        assertTrue(events[0].content.startsWith("m:"))
        
        // Verify JSON content
        val json = events[0].content.substring(2) // Remove "m:" prefix
        val parsed = objectMapper.readValue(json, Map::class.java)
        assertEquals("ok", parsed["status"])
        assertEquals(42, parsed["count"])
    }
    
    @Test
    fun `should build subscribe control message`() {
        val message = WebSocketMessageBuilder()
            .subscribe("test-channel", "prev-123")
            .build()
        
        val events = WebSocketEventParser.parse(message)
        assertEquals(1, events.size)
        assertTrue(events[0].content.startsWith("c:"))
        
        val json = events[0].content.substring(2)
        val control = objectMapper.readValue(json, Map::class.java)
        assertEquals("subscribe", control["type"])
        assertEquals("test-channel", control["channel"])
        assertEquals("prev-123", control["prev-id"])
    }
    
    @Test
    fun `should build keep-alive control message`() {
        val message = WebSocketMessageBuilder()
            .keepAlive(timeout = 30, content = "{}")
            .build()
        
        val events = WebSocketEventParser.parse(message)
        assertEquals(1, events.size)
        assertTrue(events[0].content.startsWith("c:"))
        
        val json = events[0].content.substring(2)
        val control = objectMapper.readValue(json, Map::class.java)
        assertEquals("keep-alive", control["type"])
        assertEquals(30, control["timeout"])
        assertEquals("{}", control["content"])
    }
    
    @Test
    fun `should build complex message sequence`() {
        val message = WebSocketMessageBuilder()
            .open()
            .subscribe("notifications")
            .keepAlive(timeout = 30)
            .message(mapOf("ready" to true))
            .build()
        
        val events = WebSocketEventParser.parse(message)
        assertEquals(4, events.size)
        
        assertEquals(WebSocketEventType.OPEN, events[0].type)
        assertEquals(WebSocketEventType.TEXT, events[1].type)
        assertTrue(events[1].content.startsWith("c:"))
        assertEquals(WebSocketEventType.TEXT, events[2].type)
        assertTrue(events[2].content.startsWith("c:"))
        assertEquals(WebSocketEventType.TEXT, events[3].type)
        assertTrue(events[3].content.startsWith("m:"))
    }
    
    @Test
    fun `should build PING and PONG events`() {
        val message = WebSocketMessageBuilder()
            .ping()
            .pong()
            .build()
        
        assertEquals("PING\r\nPONG\r\n", message)
    }
    
    @Test
    fun `should build CLOSE event with reason`() {
        val reason = "1000 Normal closure"
        val message = WebSocketMessageBuilder()
            .close(reason)
            .build()
        
        val expected = "CLOSE 13\r\n1000 Normal closure\r\n"
        assertEquals(expected, message)
    }
    
    @Test
    fun `should build DISCONNECT event`() {
        val message = WebSocketMessageBuilder()
            .disconnect()
            .build()
        
        assertEquals("DISCONNECT\r\n", message)
    }
}