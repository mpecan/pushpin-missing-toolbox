package io.github.mpecan.pmt.controller

import io.github.mpecan.pmt.service.PushpinService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
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

    @Test
    fun `getServerById should return 404 when server does not exist`() {
        Mockito.`when`(pushpinService.getServerById("non-existent")).thenReturn(null)

        mockMvc.perform(get("/api/pushpin/servers/non-existent"))
            .andExpect(status().isNotFound)
    }
}
