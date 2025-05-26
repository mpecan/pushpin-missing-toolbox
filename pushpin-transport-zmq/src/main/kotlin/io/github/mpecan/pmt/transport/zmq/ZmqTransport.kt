package io.github.mpecan.pmt.transport.zmq

import io.github.mpecan.pmt.client.model.Message
import io.github.mpecan.pmt.client.serialization.MessageSerializationService
import io.github.mpecan.pmt.client.serialization.MessageSerializer
import io.github.mpecan.pmt.discovery.PushpinDiscoveryManager
import io.github.mpecan.pmt.model.PushpinServer
import io.github.mpecan.pmt.transport.PushpinTransport
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * ZeroMQ transport for Pushpin servers.
 *
 * This class manages a persistent connection pool of ZMQ sockets for publishing
 * messages to multiple Pushpin servers. It uses the PUSH socket type to connect
 * to Pushpin's PULL socket, and handles socket creation, reconnection, message
 * formatting, and cleanup.
 */
class ZmqTransport(
    private val zmqProperties: ZmqTransportProperties,
    private val messageSerializer: MessageSerializer,
    private val messageSerializationService: MessageSerializationService,
    private val discoveryManager: PushpinDiscoveryManager? = null,
) : PushpinTransport {
    private val logger = LoggerFactory.getLogger(ZmqTransport::class.java)
    private val context = ZContext()
    private val sockets = ConcurrentHashMap<String, ZMQ.Socket>()
    private val lock = ReentrantReadWriteLock()
    private val executor = Executors.newCachedThreadPool()

    // For testing purposes
    private var testServers: List<PushpinServer>? = null

    /**
     * Initializes the ZMQ transport.
     */
    init {
        logger.info(
            "Initializing ZMQ transport with PUSH socket type and connection pool: ${zmqProperties.connectionPoolEnabled}",
        )
    }

    /**
     * Sets servers for testing purposes.
     */
    fun setServersForTesting(servers: List<PushpinServer>) {
        this.testServers = servers
    }

    /**
     * Gets or creates a ZMQ socket for the given server using the connection pool.
     */
    private fun getSocket(server: PushpinServer): ZMQ.Socket {
        val serverKey = server.id

        if (!zmqProperties.connectionPoolEnabled) {
            logger.debug("Connection pooling disabled, creating new socket for server: $serverKey")
            return createSocket(server)
        }

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
        val socket = context.createSocket(SocketType.PUSH)

        socket.setHWM(zmqProperties.hwm)
        socket.linger = zmqProperties.linger
        socket.setSendTimeOut(zmqProperties.sendTimeout)
        socket.setReconnectIVL(zmqProperties.reconnectIvl)
        socket.setReconnectIVLMax(zmqProperties.reconnectIvlMax)

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
     * Scheduled task to refresh the connection pool.
     */
    @Scheduled(fixedDelayString = "\${pushpin.transport.zmq.connectionPoolRefreshInterval:60000}")
    fun refreshConnectionPool() {
        if (!zmqProperties.connectionPoolEnabled) {
            return
        }

        logger.debug("Refreshing ZMQ connection pool")

        lock.writeLock().lock()
        try {
            val knownServerIds = sockets.keys.toSet()
            val serversToRemove = knownServerIds.filter { false }

            serversToRemove.forEach { serverId ->
                logger.info("Removing stale ZMQ socket for server: $serverId")
                val socket = sockets.remove(serverId)
                socket?.close()
            }

            sockets.forEach { (serverId, _) ->
                logger.debug("Connection pool contains socket for server: $serverId")
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * Publishes a message to the specified servers.
     * This is the core implementation that all other publish methods use.
     */
    private fun publishToServers(servers: List<PushpinServer>, message: Message): Mono<Boolean> {
        if (servers.isEmpty()) {
            logger.warn("No servers to publish to")
            return Mono.just(false)
        }

        logger.debug("Publishing message to channel: ${message.channel} on ${servers.size} servers")

        val pushpinMessage = messageSerializer.serialize(message)
        val dataString = "J${messageSerializationService.serialize(pushpinMessage)}"
        val data = dataString.toByteArray()

        return Mono.fromCallable {
            val results = servers.map { server ->
                try {
                    val socket = getSocket(server)
                    logger.debug(
                        "Publishing to channel: '${message.channel}' via ZMQ PUSH socket to server: ${server.id}",
                    )

                    val sendResult = socket.send(data, 0)

                    if (sendResult) {
                        logger.debug(
                            "Successfully published message to server ${server.id} on channel: ${message.channel}",
                        )
                        true
                    } else {
                        logger.error("Failed to send message to server ${server.id} on channel: ${message.channel}")
                        if (!zmqProperties.connectionPoolEnabled) {
                            socket.close()
                        }
                        false
                    }
                } catch (e: Exception) {
                    logger.error("Failed to publish message to server ${server.id}: ${e.message}", e)
                    false
                }
            }

            results.all { it }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Publishes a message to the specified servers.
     * This method is provided for backward compatibility and testing.
     */
    fun publish(servers: List<PushpinServer>, message: Message): Mono<Boolean> {
        return publishToServers(servers, message)
    }

    /**
     * Closes all ZMQ sockets and the context when the bean is destroyed.
     */
    @PreDestroy
    fun close() {
        logger.info("Closing ZMQ transport")

        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }

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

        context.close()
    }

    /**
     * Implements the PushpinTransport interface method.
     * Publishes a message to all active Pushpin servers.
     */
    override fun publish(message: Message): Mono<Boolean> {
        val servers = testServers ?: discoveryManager?.getAllServers() ?: emptyList()
        return publishToServers(servers, message)
    }
}
