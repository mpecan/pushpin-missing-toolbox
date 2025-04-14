package io.github.mpecan.pmt.model

enum class Transport {
    WebSocket,
    HttpStream,
    HttpResponse,
    HttpStreamSSE,
    HttpResponseSSE,
    LongPolling,
}
