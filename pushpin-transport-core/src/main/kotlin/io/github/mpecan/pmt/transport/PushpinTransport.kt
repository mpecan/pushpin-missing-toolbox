package io.github.mpecan.pmt.transport

import io.github.mpecan.pmt.client.model.Message
import reactor.core.publisher.Mono

interface PushpinTransport {
    fun publish(message: Message): Mono<Boolean>
}
