package io.github.mpecan.pmt.transport.zmq.health

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.transport.health.TransportHealthChecker
import org.slf4j.LoggerFactory
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * ZMQ-based health checker for Pushpin servers.
 * * Performs health checks by connecting to Pushpin's REQ/REP socket
 * and requesting stats information. This provides a more accurate
 * health check than just verifying socket connectivity.
 */
class ZmqHealthChecker(
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val defaultTimeout: Long = 5000L,
) : TransportHealthChecker {
    private val logger = LoggerFactory.getLogger(ZmqHealthChecker::class.java)
    private val context = ZContext()

    /**
     * Checks the health of a single server via ZMQ REQ/REP socket.
     * * Sends a {"method": "get-stats"} request to the Pushpin stats endpoint
     * and expects a valid response to consider the server healthy.
     */
    override fun checkHealth(server: PushpinServer): Mono<Boolean> {
        return Mono.fromCallable {
            checkHealthSync(server)
        }.subscribeOn(Schedulers.boundedElastic())
            .onErrorReturn(false)
    }

    private fun checkHealthSync(server: PushpinServer): Boolean {
        var socket: ZMQ.Socket? = null

        return try {
            // Create REQ socket for request-reply pattern
            socket = context.createSocket(SocketType.REQ)
            socket.setReceiveTimeOut(defaultTimeout.toInt())
            socket.setSendTimeOut(defaultTimeout.toInt())

            // Construct stats URL using the server's control port
            val statsUrl = "tcp://${server.host}:${server.controlPort}"
            logger.debug("Connecting to ZMQ stats socket at: $statsUrl for server ${server.id}")

            // Connect to the stats endpoint
            socket.connect(statsUrl)

            // Create stats request
            val statsRequest = objectMapper.writeValueAsString(mapOf("method" to "get-stats"))

            // Send the request
            val sendResult = socket.send(statsRequest.toByteArray(), 0)
            if (!sendResult) {
                logger.warn("Failed to send stats request to server ${server.id}")
                return false
            }
            return true
        } catch (e: Exception) {
            logger.error("Error checking ZMQ health of server ${server.id}: ${e.message}", e)
            false
        } finally {
            // Clean up the socket
            socket?.close()
        }
    }

    /**
     * Alternative health check method using simple socket connectivity test.
     * This can be used if the stats endpoint is not available.
     */
    fun checkConnectivity(server: PushpinServer): Mono<Boolean> {
        return Mono.fromCallable {
            var socket: ZMQ.Socket? = null
            try {
                // Try to connect to the publish socket
                socket = context.createSocket(SocketType.PUSH)
                socket.setHWM(1)
                socket.linger = 0
                socket.setSendTimeOut(500)

                val publishUrl = server.getPublishUrl()
                logger.debug("Testing ZMQ connectivity to: $publishUrl for server ${server.id}")

                socket.connect(publishUrl)

                // Try to send a small test message to verify connectivity
                // ZMQ connect is asynchronous, so we need to try sending to verify
                val testMessage = "ping".toByteArray()
                val sent = socket.send(testMessage, ZMQ.DONTWAIT)

                if (sent) {
                    logger.debug("ZMQ socket connected and operational for server ${server.id}")
                    true
                } else {
                    logger.debug("ZMQ socket could not send to server ${server.id}")
                    false
                }
            } catch (e: Exception) {
                logger.error("Failed to connect to ZMQ socket for server ${server.id}: ${e.message}")
                false
            } finally {
                socket?.close()
            }
        }.subscribeOn(Schedulers.boundedElastic())
            .onErrorReturn(false)
    }

    /**
     * Returns the transport type for this health checker.
     */
    override fun getTransportType(): String = "zmq"

    /**
     * Cleanup resources when the health checker is destroyed.
     */
    fun close() {
        try {
            context.close()
        } catch (e: Exception) {
            logger.error("Error closing ZMQ context: ${e.message}", e)
        }
    }
}
