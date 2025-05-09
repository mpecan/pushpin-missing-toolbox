package io.github.mpecan.pmt.service.zmq

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.PushpinMessage
import io.github.mpecan.pmt.model.PushpinServer
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * ZeroMQ publisher for Pushpin servers.
 *
 * This class manages ZMQ sockets for publishing messages to multiple Pushpin servers.
 * It uses the PUSH socket type to connect to Pushpin's PULL socket, and handles
 * socket creation, message formatting, and cleanup.
 */
@Component
class ZmqPublisher(
    private val pushpinProperties: PushpinProperties,
    private val messageSerializer: MessageSerializer,
    private val messageSerializationService: MessageSerializationService
) {
    private val logger = LoggerFactory.getLogger(ZmqPublisher::class.java)
    private val context = ZContext()
    private val sockets = ConcurrentHashMap<String, ZMQ.Socket>()
    private val executor = Executors.newCachedThreadPool()

    /**
     * Initializes the ZMQ publisher.
     */
    init {
        logger.info("Initializing ZMQ publisher with PUSH socket type")
    }

    /**
     * Gets or creates a ZMQ socket for the given server.
     */
    private fun getSocket(server: PushpinServer): ZMQ.Socket {
        return sockets.computeIfAbsent(server.id) { _ ->
            // Always use PUSH socket for Pushpin's publish endpoint (port 5560)
            // This is the correct socket type for publishing messages to Pushpin
            val socketType = SocketType.PUSH

            val socket = context.createSocket(socketType)
            socket.setHWM(pushpinProperties.zmqHwm)
            socket.linger = pushpinProperties.zmqLinger

            // For container tests, we need a small send timeout to avoid blocking
            socket.setSendTimeOut(1000)

            val publishUrl = server.getPublishUrl()
            logger.info("Connecting to ZMQ publish socket at: $publishUrl for server ID: ${server.id}")

            try {
                socket.connect(publishUrl)
                logger.info("Successfully connected to ZMQ publish socket at: $publishUrl")
            } catch (e: Exception) {
                logger.error("Failed to connect to ZMQ publish socket at: $publishUrl", e)
            }

            socket
        }
    }

    /**
     * Publishes a PushpinMessage to all active Pushpin servers using ZMQ.
     * This method handles all the ZMQ formatting requirements internally.
     *
     * @param servers List of Pushpin servers to publish to
     * @param message The PushpinMessage to publish
     * @return List of Futures with publishing results
     */
    fun publishMessage(
        servers: List<PushpinServer>,
        message: PushpinMessage
    ): List<Future<Boolean>> {
        logger.debug("Publishing message to channel: ${message.channel} on ${servers.size} servers")

        // Format the message for ZMQ
        val dataString = "J${messageSerializationService.serialize(message)}"
        val data = dataString.toByteArray()

        logger.debug("Sending message to channel: ${message.channel} on ${servers.size} servers with data length: $dataString")
        return publishRaw(servers, message.channel, data)
    }

    /**
     * Publishes a raw message to all active Pushpin servers using ZMQ.
     *
     * @param servers List of Pushpin servers to publish to
     * @param channel The channel to publish to
     * @param data The formatted data to send
     * @return List of Futures with publishing results
     */
    fun publishRaw(
        servers: List<PushpinServer>,
        channel: String,
        data: ByteArray
    ): List<Future<Boolean>> {
        return servers.map { server ->
            executor.submit<Boolean> {
                try {
                    val socket = getSocket(server)

                    logger.info("Publishing to channel: '$channel' via ZMQ PUSH socket to server: ${server.id}")

                    if (logger.isDebugEnabled) {
                        logger.debug("ZMQ raw message for ${server.id}: ${String(data)}")
                    }

                    // Send the formatted message directly
                    socket.send(data, 0)

                    logger.debug("Successfully published message to server ${server.id} on channel: $channel")
                    true
                } catch (e: Exception) {
                    logger.error(
                        "Failed to publish message to server ${server.id}: ${e.message}",
                        e
                    )
                    false
                }
            }
        }
    }


    /**
     * Publishes a Message directly to all active Pushpin servers.
     * This method handles the serialization and formatting for ZMQ in one step.
     *
     * @param servers List of Pushpin servers to publish to
     * @param message The Message to publish
     * @return List of Futures with publishing results
     */
    fun publish(servers: List<PushpinServer>, message: Message): List<Future<Boolean>> {
        logger.debug("Publishing Message to channel: ${message.channel} on ${servers.size} servers")

        val pushpinMessage = messageSerializer.serialize(message)
        return publishMessage(servers, pushpinMessage)
    }

    /**
     * Closes all ZMQ sockets and the context when the bean is destroyed.
     */
    @PreDestroy
    fun close() {
        logger.info("Closing ZMQ publisher")

        // Shutdown the executor
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        // Close all sockets
        sockets.forEach { (id, socket) ->
            logger.debug("Closing ZMQ socket for server: $id")
            socket.close()
        }
        sockets.clear()

        // Close the ZMQ context
        context.close()
    }
}