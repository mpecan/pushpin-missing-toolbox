package io.github.mpecan.pmt.client.model

/**
 * Represents the different transport mechanisms supported by Pushpin.
 * * Each transport type corresponds to a different way of delivering messages to clients:
 * - WebSocket: For real-time bidirectional communication over a persistent connection
 * - HttpStream: For streaming data over HTTP with custom content type
 * - HttpResponse: For delivering a single HTTP response
 * - HttpStreamSSE: For Server-Sent Events (SSE) streaming
 * - HttpResponseSSE: For a single SSE response
 * - LongPolling: For clients that request data and wait for a response
 */
enum class Transport {
    /**
     * WebSocket transport for real-time bidirectional communication.
     */
    WebSocket,

    /**
     * HTTP streaming transport for continuous data delivery with custom content type.
     */
    HttpStream,

    /**
     * HTTP response transport for delivering a single response.
     */
    HttpResponse,

    /**
     * HTTP streaming with Server-Sent Events (SSE) format.
     */
    HttpStreamSSE,

    /**
     * Single HTTP response with Server-Sent Events (SSE) format.
     */
    HttpResponseSSE,

    /**
     * Long polling transport where clients request data and wait for a response.
     */
    LongPolling,
}
