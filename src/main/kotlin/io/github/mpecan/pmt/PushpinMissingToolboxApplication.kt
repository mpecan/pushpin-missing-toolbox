package io.github.mpecan.pmt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PushpinMissingToolboxApplication

fun main(args: Array<String>) {
    runApplication<PushpinMissingToolboxApplication>(*args)
}
