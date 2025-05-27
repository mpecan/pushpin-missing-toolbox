# Configuration Guide

## Configuration Sources

The application supports multiple configuration sources:

1. **application.properties** - Default configuration
2. **application-{profile}.properties** - Profile-specific (dev, prod, etc.)
3. **Environment variables** - Override properties
4. **Command line arguments** - Highest priority

## Environment Variables

Any property can be set via environment variables:

```bash
# Convert dots to underscores, uppercase
PUSHPIN_SERVERS_0_HOST=pushpin-prod.example.com
PUSHPIN_SECURITY_JWT_ENABLED=true
```

## Complete Configuration Reference

### Server Configuration

```properties
# Application port
server.port=8080

# Pushpin server configuration
pushpin.servers[0].id=pushpin-1
pushpin.servers[0].host=pushpin-1
pushpin.servers[0].port=7999
pushpin.servers[0].controlPort=5564
pushpin.servers[0].publishPort=5560
pushpin.servers[0].active=true
pushpin.servers[0].weight=100
pushpin.servers[0].healthCheckPath=/status

# Add more servers as needed
pushpin.servers[1].id=pushpin-2
pushpin.servers[1].host=pushpin-2
pushpin.servers[1].port=7999
# ... etc
```

### Health Monitoring

```properties
# Health check configuration
pushpin.healthCheckEnabled=true
pushpin.healthCheckInterval=60000  # milliseconds
pushpin.defaultTimeout=5000        # milliseconds
```

### Authentication & Security

```properties
# Basic authentication
pushpin.authEnabled=false
pushpin.authSecret=changeme

# JWT Authentication
pushpin.security.jwt.enabled=false
pushpin.security.jwt.provider=symmetric  # Options: symmetric, keycloak, auth0
pushpin.security.jwt.secret=changemechangemechangemechangemechangemechangeme
pushpin.security.jwt.jwksUri=
pushpin.security.jwt.issuer=pushpin-missing-toolbox
pushpin.security.jwt.audience=pushpin-client
pushpin.security.jwt.expirationMs=3600000
pushpin.security.jwt.clientId=
pushpin.security.jwt.clientSecret=
pushpin.security.jwt.authoritiesClaim=roles
pushpin.security.jwt.authoritiesPrefix=ROLE_

# Rate limiting
pushpin.security.rateLimit.enabled=false
pushpin.security.rateLimit.capacity=100
pushpin.security.rateLimit.refillTimeInMillis=60000

# Audit logging
pushpin.security.auditLogging.enabled=true
pushpin.security.auditLogging.level=INFO

# HMAC request signing
pushpin.security.hmac.enabled=false
pushpin.security.hmac.algorithm=HmacSHA256
pushpin.security.hmac.secretKey=changeme
pushpin.security.hmac.headerName=X-Pushpin-Signature

# Encryption
pushpin.security.encryption.enabled=false
pushpin.security.encryption.algorithm=AES/GCM/NoPadding
pushpin.security.encryption.secretKey=
```

### Transport Configuration

```properties
# Transport type: http or zmq
pushpin.transport=http

# ZMQ configuration
pushpin.zmqEnabled=false
pushpin.testMode=false
pushpin.transport.zmq.connectionPoolEnabled=true
pushpin.transport.zmq.hwm=1000
pushpin.transport.zmq.linger=0
pushpin.transport.zmq.sendTimeout=1000
pushpin.transport.zmq.reconnectIvl=100
pushpin.transport.zmq.reconnectIvlMax=0
pushpin.transport.zmq.connectionPoolRefreshInterval=60000
```

### Service Discovery

```properties
# Discovery configuration
pushpin.discovery.enabled=true
pushpin.discovery.refreshInterval=PT30S

# AWS Discovery
pushpin.discovery.aws.enabled=false
pushpin.discovery.aws.region=us-east-1
pushpin.discovery.aws.tags.service=pushpin
pushpin.discovery.aws.tags.environment=production
pushpin.discovery.aws.port=7999
pushpin.discovery.aws.controlPort=5564
pushpin.discovery.aws.publishPort=5560
pushpin.discovery.aws.healthCheckPath=/status
pushpin.discovery.aws.privateIp=true
pushpin.discovery.aws.refreshCacheMinutes=5
pushpin.discovery.aws.useAutoScalingGroups=false
pushpin.discovery.aws.autoScalingGroupNames[0]=pushpin-asg
pushpin.discovery.aws.assumeRoleArn=
pushpin.discovery.aws.endpoint=  # For LocalStack testing

# Kubernetes Discovery
pushpin.discovery.kubernetes.enabled=false
pushpin.discovery.kubernetes.namespace=default
pushpin.discovery.kubernetes.labelSelector=app=pushpin
pushpin.discovery.kubernetes.fieldSelector=status.phase=Running
pushpin.discovery.kubernetes.refreshCacheSeconds=30
pushpin.discovery.kubernetes.port=7999
pushpin.discovery.kubernetes.controlPort=5564
pushpin.discovery.kubernetes.publishPort=5560
pushpin.discovery.kubernetes.healthCheckPath=/status
pushpin.discovery.kubernetes.kubeConfigPath=  # null for in-cluster
pushpin.discovery.kubernetes.useNodePort=false
pushpin.discovery.kubernetes.useService=false
pushpin.discovery.kubernetes.serviceName=pushpin
```

### Monitoring & Metrics

```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true

# Metrics
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.slo.http.server.requests=50ms,100ms,200ms,400ms,800ms,1s,2s,5s
management.metrics.tags.application=${spring.application.name}
management.metrics.tags.environment=development
```

### Logging

```properties
# Application logging
logging.level.io.github.mpecan.pmt=DEBUG
logging.level.org.zeromq=DEBUG

# Log file configuration
logging.file.name=logs/pushpin-toolbox.log
logging.file.max-size=10MB
logging.file.max-history=10
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

## Profile-Specific Configurations

### Development Profile

Create `application-dev.properties`:

```properties
# Development settings
pushpin.testMode=true
logging.level.io.github.mpecan.pmt=DEBUG
pushpin.healthCheckInterval=10000  # More frequent health checks

# Use local Pushpin servers
pushpin.servers[0].host=localhost
pushpin.servers[1].host=localhost
```

### Production Profile

Create `application-prod.properties`:

```properties
# Production settings
pushpin.authEnabled=true
pushpin.security.jwt.enabled=true
pushpin.security.rateLimit.enabled=true
pushpin.security.encryption.enabled=true

# Use discovery
pushpin.discovery.enabled=true
pushpin.discovery.aws.enabled=true

# Stricter timeouts
pushpin.defaultTimeout=2000
pushpin.healthCheckInterval=30000

# Production logging
logging.level.io.github.mpecan.pmt=INFO
logging.level.root=WARN
```

## Common Scenarios

### High Availability Setup

```properties
# Multiple servers with load balancing
pushpin.servers[0].id=pushpin-primary
pushpin.servers[0].host=pushpin1.example.com
pushpin.servers[0].weight=200  # Higher weight = more traffic

pushpin.servers[1].id=pushpin-secondary
pushpin.servers[1].host=pushpin2.example.com
pushpin.servers[1].weight=100

pushpin.servers[2].id=pushpin-backup
pushpin.servers[2].host=pushpin3.example.com
pushpin.servers[2].weight=50

# Enable ZMQ for reliable multi-server publishing
pushpin.zmqEnabled=true
pushpin.transport=zmq
```

### Secure Setup

```properties
# Enable all security features
pushpin.authEnabled=true
pushpin.security.jwt.enabled=true
pushpin.security.jwt.provider=keycloak
pushpin.security.jwt.jwksUri=https://auth.example.com/realms/main/protocol/openid-connect/certs
pushpin.security.jwt.issuer=https://auth.example.com/realms/main
pushpin.security.jwt.audience=pushpin-api

pushpin.security.rateLimit.enabled=true
pushpin.security.rateLimit.capacity=1000
pushpin.security.rateLimit.refillTimeInMillis=60000

pushpin.security.hmac.enabled=true
pushpin.security.hmac.secretKey=${HMAC_SECRET}

pushpin.security.encryption.enabled=true
pushpin.security.encryption.secretKey=${ENCRYPTION_KEY}
```

### AWS Auto-Scaling Setup

```properties
# Dynamic discovery in AWS
pushpin.discovery.enabled=true
pushpin.discovery.aws.enabled=true
pushpin.discovery.aws.region=us-east-1
pushpin.discovery.aws.useAutoScalingGroups=true
pushpin.discovery.aws.autoScalingGroupNames[0]=pushpin-asg-prod
pushpin.discovery.aws.refreshCacheMinutes=2
pushpin.discovery.aws.privateIp=true

# Disable static servers
pushpin.servers=
```

## Troubleshooting Configuration

### Debug Configuration Loading

```bash
# See which properties are loaded
java -jar app.jar --debug

# Override specific properties
java -jar app.jar \
  --pushpin.servers[0].host=debug-server \
  --logging.level.io.github.mpecan.pmt=TRACE
```

### Common Issues

1. **Servers not discovered**
   - Check `pushpin.discovery.enabled=true`
   - Verify AWS/K8s credentials
   - Check discovery refresh interval

2. **Authentication failing**
   - Verify JWT issuer and audience match
   - Check JWKS URI is accessible
   - Ensure secret keys are properly set

3. **Health checks failing**
   - Verify health check path matches Pushpin config
   - Check network connectivity
   - Increase timeout values

4. **Rate limiting too restrictive**
   - Increase capacity
   - Decrease refill time
   - Consider per-user vs global limits