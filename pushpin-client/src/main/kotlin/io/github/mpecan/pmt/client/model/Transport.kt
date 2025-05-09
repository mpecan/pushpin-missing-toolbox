package io.github.mpecan.pmt.client.model

enum class Transport {
    WebSocket,
    HttpStream,
    HttpResponse,
    HttpStreamSSE,
    HttpResponseSSE,
    LongPolling,
}
