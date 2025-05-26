package io.github.mpecan.pmt.grip.websocket

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WebSocketEventParserTest {
    
    @Test
    fun `should parse empty body`() {
        val events = WebSocketEventParser.parse("")
        assertEquals(0, events.size)
    }
    
    @Test
    fun `should parse OPEN event`() {
        val body = "OPEN\r\n"
        val events = WebSocketEventParser.parse(body)
        
        assertEquals(1, events.size)
        assertEquals(WebSocketEventType.OPEN, events[0].type)
        assertEquals("", events[0].content)
    }
    
    @Test
    fun `should parse TEXT event with content`() {
        val message = "Hello World"
        val body = "TEXT ${message.length.toString(16)}\r\n$message\r\n"
        val events = WebSocketEventParser.parse(body)
        
        assertEquals(1, events.size)
        assertEquals(WebSocketEventType.TEXT, events[0].type)
        assertEquals(message, events[0].content)
    }
    
    @Test
    fun `should parse multiple events`() {
        val message1 = "First message"
        val message2 = "Second message"
        val body = "OPEN\r\n" +
                "TEXT ${message1.length.toString(16)}\r\n$message1\r\n" +
                "TEXT ${message2.length.toString(16)}\r\n$message2\r\n" +
                "PING\r\n"
        
        val events = WebSocketEventParser.parse(body)
        
        assertEquals(4, events.size)
        assertEquals(WebSocketEventType.OPEN, events[0].type)
        assertEquals(WebSocketEventType.TEXT, events[1].type)
        assertEquals(message1, events[1].content)
        assertEquals(WebSocketEventType.TEXT, events[2].type)
        assertEquals(message2, events[2].content)
        assertEquals(WebSocketEventType.PING, events[3].type)
    }
    
    @Test
    fun `should parse CLOSE event with content`() {
        val reason = "1000 Normal closure"
        val body = "CLOSE ${reason.length.toString(16)}\r\n$reason\r\n"
        val events = WebSocketEventParser.parse(body)
        
        assertEquals(1, events.size)
        assertEquals(WebSocketEventType.CLOSE, events[0].type)
        assertEquals(reason, events[0].content)
    }
    
    @Test
    fun `should encode events`() {
        val events = listOf(
            WebSocketEvent(WebSocketEventType.OPEN),
            WebSocketEvent(WebSocketEventType.TEXT, "Hello"),
            WebSocketEvent(WebSocketEventType.PONG)
        )
        
        val encoded = WebSocketEventParser.encode(events)
        val expected = "OPEN\r\nTEXT 5\r\nHello\r\nPONG\r\n"
        
        assertEquals(expected, encoded)
    }
    
    @Test
    fun `should handle malformed events gracefully`() {
        val body = "OPEN\r\nINVALID\r\nTEXT 100\r\nShort\r\n"
        val events = WebSocketEventParser.parse(body)
        
        // Should only parse the OPEN event before encountering issues
        assertEquals(1, events.size)
        assertEquals(WebSocketEventType.OPEN, events[0].type)
    }
}