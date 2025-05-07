package io.github.mpecan.pmt.client.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageTest {

    @Test
    fun `simple creates message with channel and data only`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertNull(message.eventType)
        assertNull(message.meta)
        assertNull(message.id)
        assertNull(message.prevId)
        assertTrue(message.transports.contains(Transport.WebSocket))
        assertTrue(message.transports.contains(Transport.HttpStreamSSE))
        assertTrue(message.transports.contains(Transport.HttpResponseSSE))
        assertTrue(message.transports.contains(Transport.LongPolling))
    }
    
    @Test
    fun `event creates message with channel, event type, and data`() {
        val message = Message.event("test-channel", "test-event", "Hello, World!")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals("test-event", message.eventType)
        assertNull(message.meta)
        assertNull(message.id)
        assertNull(message.prevId)
    }
    
    @Test
    fun `withMeta creates message with channel, data, and metadata`() {
        val meta = mapOf("key" to "value")
        val message = Message.withMeta("test-channel", "Hello, World!", meta)
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertNull(message.eventType)
        assertEquals(meta, message.meta)
        assertNull(message.id)
        assertNull(message.prevId)
    }
    
    @Test
    fun `withTransports creates message with specific transports`() {
        val transports = listOf(Transport.WebSocket, Transport.HttpStream)
        val message = Message.withTransports("test-channel", "Hello, World!", transports)
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertNull(message.eventType)
        assertNull(message.meta)
        assertNull(message.id)
        assertNull(message.prevId)
        assertEquals(transports, message.transports)
    }
    
    @Test
    fun `webSocketOnly creates message with WebSocket transport only`() {
        val message = Message.webSocketOnly("test-channel", "Hello, World!")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals(listOf(Transport.WebSocket), message.transports)
    }
    
    @Test
    fun `httpStreamOnly creates message with HttpStream transport only`() {
        val message = Message.httpStreamOnly("test-channel", "Hello, World!")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals(listOf(Transport.HttpStream), message.transports)
    }
    
    @Test
    fun `sseOnly creates message with SSE transport only`() {
        val message = Message.sseOnly("test-channel", "Hello, World!")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals(listOf(Transport.HttpStreamSSE), message.transports)
    }
    
    @Test
    fun `withIds creates message with tracking IDs`() {
        val message = Message.withIds("test-channel", "Hello, World!", "msg-123", "msg-122")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals("msg-123", message.id)
        assertEquals("msg-122", message.prevId)
    }
    
    @Test
    fun `withIds creates message with id only`() {
        val message = Message.withIds("test-channel", "Hello, World!", "msg-123")
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals("msg-123", message.id)
        assertNull(message.prevId)
    }
    
    @Test
    fun `custom creates fully customized message`() {
        val meta = mapOf("key" to "value")
        val transports = listOf(Transport.WebSocket, Transport.HttpStream)
        val message = Message.custom(
            channel = "test-channel",
            data = "Hello, World!",
            eventType = "test-event",
            meta = meta,
            id = "msg-123",
            prevId = "msg-122",
            transports = transports
        )
        
        assertEquals("test-channel", message.channel)
        assertEquals("Hello, World!", message.data)
        assertEquals("test-event", message.eventType)
        assertEquals(meta, message.meta)
        assertEquals("msg-123", message.id)
        assertEquals("msg-122", message.prevId)
        assertEquals(transports, message.transports)
    }
    
    @Test
    fun `custom uses default transports when null is provided`() {
        val message = Message.custom(
            channel = "test-channel",
            data = "Hello, World!",
            transports = null
        )
        
        assertTrue(message.transports.contains(Transport.WebSocket))
        assertTrue(message.transports.contains(Transport.HttpStreamSSE))
        assertTrue(message.transports.contains(Transport.HttpResponseSSE))
        assertTrue(message.transports.contains(Transport.LongPolling))
    }
    
    @Test
    fun `addMeta adds metadata to existing message`() {
        val initialMeta = mapOf("key1" to "value1")
        val message = Message.withMeta("test-channel", "Hello, World!", initialMeta)
        
        val additionalMeta = mapOf("key2" to "value2")
        val updatedMessage = message.addMeta(additionalMeta)
        
        val expectedMeta = mapOf("key1" to "value1", "key2" to "value2")
        assertEquals(expectedMeta, updatedMessage.meta)
    }
    
    @Test
    fun `addMeta works with null initial metadata`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        val additionalMeta = mapOf("key" to "value")
        val updatedMessage = message.addMeta(additionalMeta)
        
        assertEquals(additionalMeta, updatedMessage.meta)
    }
    
    @Test
    fun `withEventType changes event type`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        val updatedMessage = message.withEventType("new-event")
        
        assertEquals("new-event", updatedMessage.eventType)
    }
    
    @Test
    fun `withTransports changes transports`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        val newTransports = listOf(Transport.WebSocket, Transport.HttpStream)
        val updatedMessage = message.withTransports(newTransports)
        
        assertEquals(newTransports, updatedMessage.transports)
    }
    
    @Test
    fun `withIds method sets message tracking IDs`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        val updatedMessage = message.withIds("msg-123", "msg-122")
        
        assertEquals("msg-123", updatedMessage.id)
        assertEquals("msg-122", updatedMessage.prevId)
    }
    
    @Test
    fun `withIds method with id only sets id and keeps prevId null`() {
        val message = Message.simple("test-channel", "Hello, World!")
        
        val updatedMessage = message.withIds("msg-123")
        
        assertEquals("msg-123", updatedMessage.id)
        assertNull(updatedMessage.prevId)
    }
}