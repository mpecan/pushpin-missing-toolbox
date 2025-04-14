package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.health.DefaultPushpinHealthChecker
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.service.PushpinService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(PushpinControllerTest.TestConfig::class)
class PushpinControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun pushpinService(): PushpinService = Mockito.mock(PushpinService::class.java)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var pushpinService: PushpinService

    @MockitoBean
    private lateinit var pushpinHealthChecker: DefaultPushpinHealthChecker

    @Test
    fun `getServerById should return 404 when server does not exist`() {
        Mockito.`when`(pushpinService.getServerById("non-existent")).thenReturn(null)

        mockMvc.perform(get("/api/pushpin/servers/non-existent"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getServerById should return 200 when server exists`() {
        val serverId = "test-server"
        val server = PushpinServer(serverId, "localhost", 7999, active = true)
        Mockito.`when`(pushpinService.getServerById(serverId)).thenReturn(server)

        mockMvc.perform(get("/api/pushpin/servers/$serverId"))
            .andExpect(status().isOk)
    }

    @Test
    fun `getHealthyServers should return 200 with healthy servers`() {
        val server1 = PushpinServer("server1", "localhost", 7999, active = true)
        val server2 = PushpinServer("server2", "localhost", 8000, active = true)
        Mockito.`when`(pushpinHealthChecker.getHealthyServers()).thenReturn(mapOf("server1" to server1, "server2" to server2))

        mockMvc.perform(get("/api/pushpin/servers/healthy"))
            .andExpect(status().isOk)
    }
}
