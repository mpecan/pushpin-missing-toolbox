# Examples

This document provides real-world examples of using the Pushpin Missing Toolbox for various use cases.

## Table of Contents

- [Basic Examples](#basic-examples)
  - [Simple Message Publishing](#simple-message-publishing)
  - [Channel Subscriptions](#channel-subscriptions)
- [Real-Time Applications](#real-time-applications)
  - [Live Dashboard](#live-dashboard)
  - [Chat Application](#chat-application)
  - [Collaborative Editor](#collaborative-editor)
- [Advanced Patterns](#advanced-patterns)
  - [Event Sourcing](#event-sourcing)
  - [Message Ordering](#message-ordering)
  - [Presence Management](#presence-management)
- [Integration Examples](#integration-examples)
  - [Spring Boot Integration](#spring-boot-integration)
  - [React Integration](#react-integration)
  - [Mobile Applications](#mobile-applications)

## Basic Examples

### Simple Message Publishing

#### Publishing a Text Message

```bash
curl -X POST http://localhost:8080/api/pushpin/publish \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "news",
    "data": {
      "title": "Breaking News",
      "content": "Important announcement",
      "timestamp": "2024-01-15T10:30:00Z"
    }
  }'
```

#### Publishing with Event Type (SSE)

```bash
curl -X POST http://localhost:8080/api/pushpin/publish/notifications?event=alert \
  -H "Content-Type: application/json" \
  -d '{
    "level": "warning",
    "message": "Server maintenance scheduled",
    "time": "2024-01-15T22:00:00Z"
  }'
```

### Channel Subscriptions

#### Server-Sent Events (SSE)

```javascript
// JavaScript client
const eventSource = new EventSource('/api/events/news');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received news:', data);
};

eventSource.addEventListener('alert', (event) => {
  const alert = JSON.parse(event.data);
  showNotification(alert.message);
});

eventSource.onerror = (error) => {
  console.error('Connection error:', error);
  // EventSource will automatically reconnect
};
```

#### WebSocket Connection

```javascript
// WebSocket client
const ws = new WebSocket('ws://localhost:7999/api/ws/chat');

ws.onopen = () => {
  console.log('Connected to chat');
  ws.send(JSON.stringify({
    type: 'join',
    user: 'Alice'
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  displayMessage(message);
};

ws.onclose = () => {
  console.log('Disconnected from chat');
  // Implement reconnection logic
};
```

## Real-Time Applications

### Live Dashboard

A complete example of a real-time metrics dashboard:

#### Backend - Metrics Publisher

```kotlin
@Component
class MetricsPublisher(
    private val pushpinService: PushpinService
) {
    
    @Scheduled(fixedDelay = 1000)
    fun publishMetrics() {
        val metrics = collectSystemMetrics()
        
        val message = Message.simple(
            channel = "metrics",
            data = mapOf(
                "timestamp" to Instant.now(),
                "cpu" to metrics.cpuUsage,
                "memory" to metrics.memoryUsage,
                "requests" to metrics.requestCount,
                "responseTime" to metrics.avgResponseTime
            )
        )
        
        pushpinService.publishMessage(message)
            .subscribe(
                { success -> 
                    if (!success) log.warn("Failed to publish metrics") 
                },
                { error -> 
                    log.error("Error publishing metrics", error) 
                }
            )
    }
    
    private fun collectSystemMetrics(): SystemMetrics {
        val runtime = Runtime.getRuntime()
        val cpuUsage = (ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean)
            .processCpuLoad * 100
        
        return SystemMetrics(
            cpuUsage = cpuUsage,
            memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100,
            requestCount = requestCounter.get(),
            avgResponseTime = calculateAverageResponseTime()
        )
    }
}
```

#### Frontend - React Dashboard

```jsx
function MetricsDashboard() {
  const [metrics, setMetrics] = useState({
    cpu: 0,
    memory: 0,
    requests: 0,
    responseTime: 0
  });
  
  useEffect(() => {
    const eventSource = new EventSource('/api/events/metrics');
    
    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setMetrics(data);
    };
    
    return () => eventSource.close();
  }, []);
  
  return (
    <div className="dashboard">
      <MetricCard title="CPU Usage" value={`${metrics.cpu.toFixed(1)}%`} />
      <MetricCard title="Memory" value={`${metrics.memory.toFixed(1)}%`} />
      <MetricCard title="Requests/sec" value={metrics.requests} />
      <MetricCard title="Response Time" value={`${metrics.responseTime}ms`} />
    </div>
  );
}
```

### Chat Application

Complete chat room implementation with user presence:

#### Backend - Chat Service

```kotlin
@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val pushpinService: PushpinService
) {
    
    private val activeUsers = ConcurrentHashMap<String, UserInfo>()
    
    @PostMapping("/join/{room}")
    fun joinRoom(
        @PathVariable room: String,
        @RequestBody user: UserInfo
    ): ResponseEntity<Any> {
        activeUsers[user.id] = user
        
        // Notify others about new user
        val joinMessage = Message(
            channel = "chat-$room",
            formats = WebSocketFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "type" to "user-joined",
                        "user" to user,
                        "timestamp" to Instant.now()
                    )
                )
            )
        )
        
        pushpinService.publishMessage(joinMessage).subscribe()
        
        return ResponseEntity.ok(mapOf(
            "room" to room,
            "users" to activeUsers.values
        ))
    }
    
    @PostMapping("/message/{room}")
    fun sendMessage(
        @PathVariable room: String,
        @RequestBody chatMessage: ChatMessage
    ): ResponseEntity<Any> {
        val message = Message(
            channel = "chat-$room",
            formats = WebSocketFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "type" to "message",
                        "id" to UUID.randomUUID().toString(),
                        "userId" to chatMessage.userId,
                        "text" to chatMessage.text,
                        "timestamp" to Instant.now()
                    )
                )
            )
        )
        
        val result = pushpinService.publishMessage(message).block()
        
        return if (result == true) {
            ResponseEntity.ok(mapOf("success" to true))
        } else {
            ResponseEntity.status(500).body(mapOf("error" to "Failed to send message"))
        }
    }
    
    @PostMapping("/typing/{room}")
    fun setTypingStatus(
        @PathVariable room: String,
        @RequestParam userId: String,
        @RequestParam typing: Boolean
    ): ResponseEntity<Any> {
        val message = Message(
            channel = "chat-$room",
            formats = WebSocketFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "type" to "typing",
                        "userId" to userId,
                        "typing" to typing
                    )
                )
            )
        )
        
        pushpinService.publishMessage(message).subscribe()
        
        return ResponseEntity.ok().build()
    }
}
```

#### Frontend - Chat Client

```javascript
class ChatClient {
  constructor(room, userId) {
    this.room = room;
    this.userId = userId;
    this.ws = null;
    this.messageHandlers = [];
    this.connect();
  }
  
  connect() {
    this.ws = new WebSocket(`ws://localhost:7999/api/ws/chat-${this.room}`);
    
    this.ws.onopen = () => {
      console.log('Connected to chat room');
      this.join();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onclose = () => {
      console.log('Disconnected, reconnecting...');
      setTimeout(() => this.connect(), 3000);
    };
  }
  
  join() {
    fetch(`/api/chat/join/${this.room}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: this.userId,
        name: this.userName,
        avatar: this.userAvatar
      })
    });
  }
  
  sendMessage(text) {
    fetch(`/api/chat/message/${this.room}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: this.userId,
        text: text
      })
    });
  }
  
  setTyping(isTyping) {
    fetch(`/api/chat/typing/${this.room}?userId=${this.userId}&typing=${isTyping}`, {
      method: 'POST'
    });
  }
  
  handleMessage(message) {
    switch(message.type) {
      case 'user-joined':
        this.onUserJoined(message.user);
        break;
      case 'message':
        this.onMessage(message);
        break;
      case 'typing':
        this.onTypingStatus(message.userId, message.typing);
        break;
    }
  }
  
  onMessage(handler) {
    this.messageHandlers.push(handler);
  }
}

// Usage
const chat = new ChatClient('general', 'user123');

chat.onMessage((message) => {
  displayChatMessage(message);
});
```

### Collaborative Editor

Real-time collaborative document editing:

```kotlin
@Component
class CollaborativeDocumentService(
    private val pushpinService: PushpinService
) {
    
    fun broadcastOperation(
        documentId: String,
        operation: DocumentOperation
    ) {
        val message = Message(
            channel = "doc-$documentId",
            formats = WebSocketFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "type" to "operation",
                        "op" to operation,
                        "userId" to operation.userId,
                        "version" to operation.version
                    )
                )
            )
        )
        
        pushpinService.publishMessage(message).subscribe()
    }
    
    fun broadcastCursor(
        documentId: String,
        userId: String,
        position: CursorPosition
    ) {
        val message = Message(
            channel = "doc-$documentId-cursors",
            formats = WebSocketFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "userId" to userId,
                        "position" to position,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            )
        )
        
        pushpinService.publishMessage(message).subscribe()
    }
}
```

## Advanced Patterns

### Event Sourcing

Implementing event sourcing with message ordering:

```kotlin
@Service
class EventSourcingService(
    private val pushpinService: PushpinService
) {
    
    private val eventSequence = AtomicLong(0)
    private val lastEventId = AtomicReference<String>()
    
    fun publishEvent(
        aggregateId: String,
        eventType: String,
        eventData: Any
    ): Mono<Boolean> {
        val eventId = "${aggregateId}-${eventSequence.incrementAndGet()}"
        val prevId = lastEventId.getAndSet(eventId)
        
        val event = Event(
            id = eventId,
            aggregateId = aggregateId,
            type = eventType,
            data = eventData,
            sequence = eventSequence.get(),
            timestamp = Instant.now()
        )
        
        val message = Message(
            channel = "events-$aggregateId",
            id = eventId,
            prevId = prevId,
            formats = HttpStreamFormat(
                content = ObjectMapper().writeValueAsString(event)
            )
        )
        
        return pushpinService.publishMessage(message)
            .doOnSuccess { success ->
                if (success) {
                    // Store event in event store
                    eventStore.save(event)
                }
            }
    }
}
```

### Message Ordering

Ensuring ordered message delivery:

```kotlin
class OrderedMessagePublisher(
    private val pushpinService: PushpinService
) {
    
    private val messageQueues = ConcurrentHashMap<String, MessageQueue>()
    
    fun publishOrdered(
        channel: String,
        data: Any
    ): Mono<Boolean> {
        val queue = messageQueues.computeIfAbsent(channel) { 
            MessageQueue(channel) 
        }
        
        return queue.publish(data)
    }
    
    inner class MessageQueue(private val channel: String) {
        private val queue = LinkedBlockingQueue<QueuedMessage>()
        private val processing = AtomicBoolean(false)
        private var lastId: String? = null
        
        fun publish(data: Any): Mono<Boolean> {
            val messageId = UUID.randomUUID().toString()
            val queuedMessage = QueuedMessage(messageId, data)
            
            queue.offer(queuedMessage)
            processQueue()
            
            return queuedMessage.result
        }
        
        private fun processQueue() {
            if (processing.compareAndSet(false, true)) {
                GlobalScope.launch {
                    try {
                        while (queue.isNotEmpty()) {
                            val queuedMessage = queue.poll() ?: break
                            
                            val message = Message(
                                channel = channel,
                                id = queuedMessage.id,
                                prevId = lastId,
                                data = queuedMessage.data
                            )
                            
                            val success = pushpinService.publishMessage(message).awaitSingle()
                            
                            if (success) {
                                lastId = queuedMessage.id
                                queuedMessage.complete(true)
                            } else {
                                queuedMessage.complete(false)
                            }
                        }
                    } finally {
                        processing.set(false)
                        // Check if new messages arrived while processing
                        if (queue.isNotEmpty()) {
                            processQueue()
                        }
                    }
                }
            }
        }
    }
}
```

### Presence Management

Managing user presence in channels:

```kotlin
@Component
class PresenceManager(
    private val pushpinService: PushpinService,
    private val scheduler: TaskScheduler
) {
    
    private val presenceMap = ConcurrentHashMap<String, MutableMap<String, UserPresence>>()
    private val heartbeats = ConcurrentHashMap<String, ScheduledFuture<*>>()
    
    fun joinChannel(channel: String, userId: String, userInfo: Any) {
        val presence = UserPresence(
            userId = userId,
            userInfo = userInfo,
            joinedAt = Instant.now(),
            lastSeen = Instant.now()
        )
        
        presenceMap.computeIfAbsent(channel) { 
            ConcurrentHashMap() 
        }[userId] = presence
        
        // Broadcast join event
        broadcastPresenceUpdate(channel, "joined", presence)
        
        // Start heartbeat
        startHeartbeat(channel, userId)
    }
    
    fun leaveChannel(channel: String, userId: String) {
        presenceMap[channel]?.remove(userId)?.let { presence ->
            broadcastPresenceUpdate(channel, "left", presence)
        }
        
        // Stop heartbeat
        heartbeats.remove("$channel:$userId")?.cancel(false)
    }
    
    private fun startHeartbeat(channel: String, userId: String) {
        val heartbeatTask = scheduler.scheduleAtFixedRate(
            {
                presenceMap[channel]?.get(userId)?.let { presence ->
                    presence.lastSeen = Instant.now()
                    
                    // Check for stale presences
                    cleanupStalePresences(channel)
                }
            },
            Duration.ofSeconds(30)
        )
        
        heartbeats["$channel:$userId"] = heartbeatTask
    }
    
    private fun cleanupStalePresences(channel: String) {
        val now = Instant.now()
        val timeout = Duration.ofMinutes(2)
        
        presenceMap[channel]?.entries?.removeIf { (userId, presence) ->
            if (Duration.between(presence.lastSeen, now) > timeout) {
                broadcastPresenceUpdate(channel, "timeout", presence)
                heartbeats.remove("$channel:$userId")?.cancel(false)
                true
            } else {
                false
            }
        }
    }
    
    private fun broadcastPresenceUpdate(
        channel: String, 
        event: String, 
        presence: UserPresence
    ) {
        val message = Message(
            channel = "$channel-presence",
            formats = HttpStreamFormat(
                content = ObjectMapper().writeValueAsString(
                    mapOf(
                        "event" to event,
                        "userId" to presence.userId,
                        "userInfo" to presence.userInfo,
                        "timestamp" to Instant.now()
                    )
                )
            )
        )
        
        pushpinService.publishMessage(message).subscribe()
    }
}
```

## Integration Examples

### Spring Boot Integration

Complete Spring Boot application with Pushpin:

```kotlin
@SpringBootApplication
@EnableScheduling
class RealtimeApplication

@Configuration
class PushpinConfig {
    
    @Bean
    fun pushpinServers(): List<PushpinServer> {
        return listOf(
            PushpinServer(
                id = "pushpin-1",
                host = "localhost",
                port = 7999,
                publishPort = 5560,
                controlPort = 5563
            ),
            PushpinServer(
                id = "pushpin-2",
                host = "localhost",
                port = 7998,
                publishPort = 5561,
                controlPort = 5564
            )
        )
    }
    
    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
            }
        }
    }
}

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val pushpinService: PushpinService
) {
    
    @PostMapping("/send")
    fun sendNotification(
        @RequestBody notification: Notification,
        @RequestParam(required = false) userId: String?
    ): Mono<ResponseEntity<Any>> {
        val channel = userId?.let { "user-$it" } ?: "global"
        
        val message = Message(
            channel = channel,
            formats = HttpStreamFormat(
                content = ObjectMapper().writeValueAsString(notification),
                contentType = "application/json"
            )
        )
        
        return pushpinService.publishMessage(message)
            .map { success ->
                if (success) {
                    ResponseEntity.ok(mapOf("status" to "sent"))
                } else {
                    ResponseEntity.status(500).body(mapOf("error" to "Failed to send"))
                }
            }
    }
}
```

### React Integration

React hooks for real-time subscriptions:

```jsx
// useEventSource.js
function useEventSource(url) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [readyState, setReadyState] = useState(0);
  
  useEffect(() => {
    const eventSource = new EventSource(url);
    
    eventSource.onopen = () => setReadyState(eventSource.readyState);
    
    eventSource.onmessage = (event) => {
      setData(JSON.parse(event.data));
    };
    
    eventSource.onerror = (error) => {
      setError(error);
      setReadyState(eventSource.readyState);
    };
    
    return () => {
      eventSource.close();
    };
  }, [url]);
  
  return { data, error, readyState };
}

// NotificationComponent.jsx
function NotificationComponent({ userId }) {
  const { data: notification } = useEventSource(
    `/api/events/user-${userId}`
  );
  
  return (
    <div>
      {notification && (
        <Alert severity={notification.severity}>
          {notification.message}
        </Alert>
      )}
    </div>
  );
}
```

### Mobile Applications

React Native implementation:

```javascript
// PushpinClient.js
class PushpinClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.eventSources = new Map();
  }
  
  subscribe(channel, onMessage, onError) {
    const url = `${this.baseUrl}/api/events/${channel}`;
    const eventSource = new RNEventSource(url);
    
    eventSource.addEventListener('message', (event) => {
      const data = JSON.parse(event.data);
      onMessage(data);
    });
    
    eventSource.addEventListener('error', (error) => {
      onError(error);
      // Implement exponential backoff reconnection
      this.reconnect(channel, onMessage, onError);
    });
    
    this.eventSources.set(channel, eventSource);
    
    return () => {
      eventSource.removeAllListeners();
      eventSource.close();
      this.eventSources.delete(channel);
    };
  }
  
  publish(channel, data) {
    return fetch(`${this.baseUrl}/api/pushpin/publish/${channel}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  }
  
  reconnect(channel, onMessage, onError, delay = 1000) {
    setTimeout(() => {
      if (!this.eventSources.has(channel)) {
        this.subscribe(channel, onMessage, onError);
      }
    }, delay);
  }
}

// Usage in React Native
const pushpin = new PushpinClient('https://api.example.com');

export function useRealtimeData(channel) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    const unsubscribe = pushpin.subscribe(
      channel,
      (data) => setData(data),
      (error) => setError(error)
    );
    
    return unsubscribe;
  }, [channel]);
  
  return { data, error };
}
```

## Best Practices

### Error Handling

Always implement proper error handling and reconnection logic:

```javascript
class ResilientEventSource {
  constructor(url, options = {}) {
    this.url = url;
    this.options = options;
    this.reconnectDelay = options.reconnectDelay || 1000;
    this.maxReconnectDelay = options.maxReconnectDelay || 30000;
    this.reconnectAttempts = 0;
    this.connect();
  }
  
  connect() {
    this.eventSource = new EventSource(this.url);
    
    this.eventSource.onopen = () => {
      console.log('Connected to', this.url);
      this.reconnectAttempts = 0;
      this.reconnectDelay = this.options.reconnectDelay || 1000;
    };
    
    this.eventSource.onerror = (error) => {
      console.error('Connection error:', error);
      this.eventSource.close();
      
      // Exponential backoff
      const delay = Math.min(
        this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
        this.maxReconnectDelay
      );
      
      console.log(`Reconnecting in ${delay}ms...`);
      setTimeout(() => this.connect(), delay);
      
      this.reconnectAttempts++;
    };
    
    // Forward events
    this.eventSource.onmessage = this.options.onmessage;
    
    // Forward custom events
    if (this.options.events) {
      Object.entries(this.options.events).forEach(([event, handler]) => {
        this.eventSource.addEventListener(event, handler);
      });
    }
  }
  
  close() {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }
}
```

### Security Considerations

Implement authentication and authorization:

```kotlin
@Component
class SecureMessagePublisher(
    private val pushpinService: PushpinService,
    private val jwtDecoder: JwtDecoder
) {
    
    fun publishToUserChannel(token: String, message: Any): Mono<Boolean> {
        return Mono.fromCallable { jwtDecoder.decode(token) }
            .map { jwt ->
                val userId = jwt.getClaimAsString("sub")
                val allowedChannels = jwt.getClaimAsStringList("channels")
                
                UserContext(userId, allowedChannels)
            }
            .flatMap { context ->
                val channel = "user-${context.userId}"
                
                if (context.allowedChannels.contains(channel)) {
                    pushpinService.publishMessage(
                        Message.simple(channel, message)
                    )
                } else {
                    Mono.just(false)
                }
            }
            .onErrorReturn(false)
    }
}
```

### Performance Optimization

Batch messages for better performance:

```kotlin
class BatchingMessagePublisher(
    private val pushpinService: PushpinService,
    private val batchSize: Int = 100,
    private val batchTimeout: Duration = Duration.ofMillis(100)
) {
    
    private val messageBuffer = ConcurrentHashMap<String, MutableList<Any>>()
    private val scheduler = Executors.newScheduledThreadPool(1)
    
    init {
        scheduler.scheduleAtFixedRate(
            ::flushBuffers,
            batchTimeout.toMillis(),
            batchTimeout.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }
    
    fun publish(channel: String, data: Any) {
        messageBuffer.computeIfAbsent(channel) { 
            Collections.synchronizedList(mutableListOf()) 
        }.add(data)
        
        if (messageBuffer[channel]?.size ?: 0 >= batchSize) {
            flushChannel(channel)
        }
    }
    
    private fun flushBuffers() {
        messageBuffer.keys.forEach { channel ->
            flushChannel(channel)
        }
    }
    
    private fun flushChannel(channel: String) {
        val messages = messageBuffer.remove(channel) ?: return
        
        if (messages.isNotEmpty()) {
            val batchMessage = Message.simple(
                channel = channel,
                data = mapOf(
                    "batch" to true,
                    "messages" to messages,
                    "count" to messages.size,
                    "timestamp" to Instant.now()
                )
            )
            
            pushpinService.publishMessage(batchMessage)
                .subscribe(
                    { success -> 
                        if (!success) {
                            log.warn("Failed to publish batch to $channel")
                        }
                    },
                    { error -> 
                        log.error("Error publishing batch", error)
                    }
                )
        }
    }
}
```

## Additional Resources

- [Pushpin Documentation](https://pushpin.org/docs/)
- [GRIP Protocol Specification](https://pushpin.org/docs/protocols/grip/)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Server-Sent Events API](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [Testing Guide](Testing.md) - Integration testing examples