package io.github.mpecan.pmt.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PushpinServerTest {

    @Test
    fun `getBaseUrl should return correct URL`() {
        val server = PushpinServer(
            id = "test-server",
            host = "localhost",
            port = 7999
        )
        
        assertEquals("http://localhost:7999", server.getBaseUrl())
    }

    @Test
    fun `getControlUrl should return correct URL`() {
        val server = PushpinServer(
            id = "test-server",
            host = "localhost",
            port = 7999,
            controlPort = 5564
        )
        
        assertEquals("http://localhost:5564", server.getControlUrl())
    }

    @Test
    fun `getPublishUrl should return correct URL`() {
        val server = PushpinServer(
            id = "test-server",
            host = "localhost",
            port = 7999,
            publishPort = 5560
        )
        
        assertEquals("tcp://localhost:5560", server.getPublishUrl())
    }

    @Test
    fun `getHealthCheckUrl should return correct URL`() {
        val server = PushpinServer(
            id = "test-server",
            host = "localhost",
            port = 7999,
            healthCheckPath = "/status"
        )
        
        assertEquals("http://localhost:7999/status", server.getHealthCheckUrl())
    }

    @Test
    fun `toUri should return correct URI`() {
        val server = PushpinServer(
            id = "test-server",
            host = "localhost",
            port = 7999
        )
        
        assertEquals("http://localhost:7999", server.toUri().toString())
    }
}