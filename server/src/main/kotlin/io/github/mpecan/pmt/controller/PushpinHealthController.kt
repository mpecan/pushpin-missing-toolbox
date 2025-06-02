package io.github.mpecan.pmt.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class PushpinHealthController {
    @GetMapping("/check")
    fun healthCheck(): String = "OK"
}
