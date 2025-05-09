package io.github.mpecan.pmt.service.zmq

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.config.PushpinProperties
import io.github.mpecan.pmt.model.PushpinMessage
import io.github.mpecan.pmt.model.PushpinServer
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * ZeroMQ publisher for Pushpin servers.
 *
 * This class manages a persistent connection pool of ZMQ sockets for publishing
 * messages to multiple Pushpin servers. It uses the PUSH socket type to connect
 * to Pushpin's PULL socket, and handles socket creation, reconnection, message
 * formatting, and cleanup.
 */
@Component
class ZmqPublisher(
    private val pushpinProperties: PushpinProperties,
    private val messageSerializer: MessageSerializer,
    private val messageSerializationService: MessageSerializationService,
) {
    private val logger = LoggerFactory.getLogger(ZmqPublisher::class.java)
    private val context = ZContext()
    private val sockets = ConcurrentHashMap<String, ZMQ.Socket>()
    private val lock = ReentrantReadWriteLock()
    private val executor = Executors.newCachedThreadPool()

    /**
     * Initializes the ZMQ publisher.
     */
    init {
        logger.info(
            "Initializing ZMQ publisher with PUSH socket type and connection pool: ${pushpinProperties.zmqConnectionPoolEnabled}",
        )
    }

    /**
     * Gets or creates a ZMQ socket for the given server using the connection pool.
     *
     * If the connection pool is enabled, this will reuse existing sockets.
     * If the connection pool is disabled, this will create a new socket each time.
     */
    private fun getSocket(server: PushpinServer): ZMQ.Socket {
        val serverKey = server.id

        // If connection pooling is disabled, create a new socket each time
        if (!pushpinProperties.zmqConnectionPoolEnabled) {
            logger.debug("Connection pooling disabled, creating new socket for server: $serverKey")
            return createSocket(server)
        }

        // Otherwise, use the connection pool
        return sockets.computeIfAbsent(serverKey) {
            logger.info("Creating new pooled socket for server: $serverKey")
            createSocket(server)
        }
    }

    /**
     * Creates a new ZMQ socket for the given server with configured parameters.
     */
    private fun createSocket(server: PushpinServer): ZMQ.Socket {
        val publishUrl = server.getPublishUrl()

        // Always use PUSH socket for Pushpin's publish endpoint
        val socket = context.createSocket(SocketType.PUSH)

        // Configure socket options from properties
        socket.setHWM(pushpinProperties.zmqHwm)
        socket.linger = pushpinProperties.zmqLinger
        socket.setSendTimeOut(pushpinProperties.zmqSendTimeout)

        // Set reconnection parameters
        socket.setReconnectIVL(pushpinProperties.zmqReconnectIvl)
        socket.setReconnectIVLMax(pushpinProperties.zmqReconnectIvlMax)

        logger.info("Connecting to ZMQ publish socket at: $publishUrl for server ID: ${server.id}")

        try {
            socket.connect(publishUrl)
            logger.info("Successfully connected to ZMQ publish socket at: $publishUrl")
        } catch (e: Exception) {
            logger.error("Failed to connect to ZMQ publish socket at: $publishUrl", e)
        }

        return socket
    }

    /**
     * Scheduled task to refresh the connection pool by removing stale connections
     * and ensuring connections to all active servers exist.
     */
    @Scheduled(fixedDelayString = "\${pushpin.zmqConnectionPoolRefreshInterval:60000}")
    fun refreshConnectionPool() {
        if (!pushpinProperties.zmqConnectionPoolEnabled) {
            return
        }

        logger.debug("Refreshing ZMQ connection pool")

        // Use a write lock to safely update the connection pool
        lock.writeLock().lock()
        try {
            // Get a snapshot of current socket keys
            val knownServerIds = sockets.keys.toSet()

            // Close and remove sockets for servers that are no longer active
            // or have been removed from the pool
            val serversToRemove = knownServerIds.filter { serverId ->
                // We can't directly check server activity here without a discovery manager,
                // but this prepares for future enhancements
                false // In a future version, check against active servers
            }

            serversToRemove.forEach { serverId ->
                logger.info("Removing stale ZMQ socket for server: $serverId")
                val socket = sockets.remove(serverId)
                socket?.close()
            }

            // Log current connections - ZMQ PUSH sockets don't have a direct way to check connection status
            // but we can use this to periodically log socket information
            sockets.forEach { (serverId, _) ->
                logger.debug("Connection pool contains socket for server: $serverId")
            }
        } finally {
            lock.writeLock().unlock()
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
    fun publishMessage(servers: List<PushpinServer>, message: PushpinMessage): List<Future<Boolean>> {
        logger.debug("Publishing message to channel: ${message.channel} on ${servers.size} servers")

        // Format the message for ZMQ
        val dataString = "J${messageSerializationService.serialize(message)}"
        val data = dataString.toByteArray()

        logger.debug(
            "Sending message to channel: ${message.channel} on ${servers.size} servers with data length: ${data.size} bytes",
        )
        return publishRaw(servers, message.channel, data)
    }

    /**
     * Publishes a PushpinMessage to all active Pushpin servers using ZMQ with a reactive API.
     * Returns a Mono that completes when all messages have been sent.
     *
     * @param servers List of Pushpin servers to publish to
     * @param message The PushpinMessage to publish
     * @return Mono<Boolean> that completes with true if all messages were sent successfully, false otherwise
     */
    fun publishMessageReactive(servers: List<PushpinServer>, message: PushpinMessage): Mono<Boolean> {
        if (servers.isEmpty()) {
            logger.warn("No servers to publish to")
            return Mono.just(false)
        }

        logger.debug("Publishing message to channel: ${message.channel} on ${servers.size} servers")

        // Format the message for ZMQ
        val dataString = "J${messageSerializationService.serialize(message)}"
        val data = dataString.toByteArray()

        return publishRawReactive(servers, message.channel, data)
    }

    /**
     * Publishes a raw message to all active Pushpin servers using ZMQ.
     *
     * @param servers List of Pushpin servers to publish to
     * @param channel The channel to publish to
     * @param data The formatted data to send
     * @return List of Futures with publishing results
     */
    fun publishRaw(servers: List<PushpinServer>, channel: String, data: ByteArray): List<Future<Boolean>> {
        return servers.map { server ->
            executor.submit<Boolean> {
                try {
                    // Get a socket from the connection pool
                    val socket = getSocket(server)

                    logger.debug("Publishing to channel: '$channel' via ZMQ PUSH socket to server: ${server.id}")

                    if (logger.isDebugEnabled) {
                        logger.debug("ZMQ raw message for ${server.id}: ${data.size} bytes")
                    }

                    // Send the formatted message directly
                    val sendResult = socket.send(data, 0)

                    if (sendResult) {
                        logger.debug("Successfully published message to server ${server.id} on channel: $channel")
                        true
                    } else {
                        logger.error("Failed to send message to server ${server.id} on channel: $channel")
                        // If connection pooling is enabled, keep the socket for future reconnection attempts
                        // If disabled, close it immediately
                        if (!pushpinProperties.zmqConnectionPoolEnabled) {
                            socket.close()
                        }
                        false
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Failed to publish message to server ${server.id}: ${e.message}",
                        e,
                    )
                    false
                }
            }
        }
    }

    /**
     * Publishes a raw message to all active Pushpin servers using ZMQ with a reactive API.
     * Returns a Mono that completes when all messages have been sent.
     *
     * @param servers List of Pushpin servers to publish to
     * @param channel The channel to publish to
     * @param data The formatted data to send
     * @return Mono<Boolean> that completes with true if all messages were sent successfully, false otherwise
     */
    fun publishRawReactive(servers: List<PushpinServer>, channel: String, data: ByteArray): Mono<Boolean> {
        // Create a mono that handles the publishing on a bounded elastic scheduler
        return Mono.fromCallable {
            val results = servers.map { server ->
                try {
                    // Get a socket from the connection pool
                    val socket = getSocket(server)

                    logger.debug("Publishing to channel: '$channel' via ZMQ PUSH socket to server: ${server.id}")

                    // Send the formatted message directly
                    val sendResult = socket.send(data, 0)

                    if (sendResult) {
                        logger.debug("Successfully published message to server ${server.id} on channel: $channel")
                        true
                    } else {
                        logger.error("Failed to send message to server ${server.id} on channel: $channel")
                        // If connection pooling is enabled, keep the socket for future reconnection attempts
                        // If disabled, close it immediately
                        if (!pushpinProperties.zmqConnectionPoolEnabled) {
                            socket.close()
                        }
                        false
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Failed to publish message to server ${server.id}: ${e.message}",
                        e,
                    )
                    false
                }
            }

            // Return true only if all sends were successful
            results.all { it }
        }.subscribeOn(Schedulers.boundedElastic())
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
     * Publishes a Message directly to all active Pushpin servers with a reactive API.
     * This method handles the serialization and formatting for ZMQ in one step.
     *
     * @param servers List of Pushpin servers to publish to
     * @param message The Message to publish
     * @return Mono<Boolean> that completes with true if all messages were sent successfully, false otherwise
     */
    fun publishReactive(servers: List<PushpinServer>, message: Message): Mono<Boolean> {
        logger.debug("Publishing Message to channel: ${message.channel} on ${servers.size} servers")

        val pushpinMessage = messageSerializer.serialize(message)
        return publishMessageReactive(servers, pushpinMessage)
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
        lock.writeLock().lock()
        try {
            sockets.forEach { (id, socket) ->
                logger.debug("Closing ZMQ socket for server: $id")
                socket.close()
            }
            sockets.clear()
        } finally {
            lock.writeLock().unlock()
        }

        // Close the ZMQ context
        context.close()
    }
}
