# Pushpin Kubernetes Discovery Module

This module provides Kubernetes-based discovery for Pushpin servers running as pods in a Kubernetes cluster. It supports discovering pods by labels and from specific namespaces.

## Usage

To use this module, add it as a dependency to your project:

```kotlin
// Gradle (Kotlin DSL)
implementation("io.github.mpecan:pushpin-missing-toolbox-discovery-kubernetes:0.0.1-SNAPSHOT")
```

The module uses Spring Boot's auto-configuration to automatically set up Kubernetes discovery when:
1. The Kubernetes Java client is on the classpath
2. The `pushpin.discovery.kubernetes.enabled` property is set to `true`

## Configuration

The Kubernetes discovery mechanism can be configured using the following properties:

```properties
# Enable Kubernetes discovery (disabled by default)
pushpin.discovery.kubernetes.enabled=true

# Kubernetes namespace to search for pods (null means all namespaces)
pushpin.discovery.kubernetes.namespace=default

# Label selector to filter pods
pushpin.discovery.kubernetes.labelSelector=app=pushpin

# Field selector to filter pods
pushpin.discovery.kubernetes.fieldSelector=status.phase=Running

# Path to kubeconfig file (null for in-cluster configuration)
# pushpin.discovery.kubernetes.kubeConfigPath=/path/to/kubeconfig

# How often to refresh the pods cache (in seconds)
pushpin.discovery.kubernetes.refreshCacheSeconds=30

# Whether to check pod health status
pushpin.discovery.kubernetes.healthCheckEnabled=true

# Default ports for discovered Pushpin servers (if not specified by pod annotations)
pushpin.discovery.kubernetes.port=7999
pushpin.discovery.kubernetes.controlPort=5564
pushpin.discovery.kubernetes.publishPort=5560

# Health check path
pushpin.discovery.kubernetes.healthCheckPath=/status

# Use NodePort for accessing services (if false, use pod IP directly)
pushpin.discovery.kubernetes.useNodePort=false

# Whether to discover services instead of pods
pushpin.discovery.kubernetes.useService=false

# Name of the service to discover (only used when useService is true)
pushpin.discovery.kubernetes.serviceName=pushpin
```

## Customization

This module is designed to be customizable. You can provide your own implementations of the following interfaces:

1. `KubernetesPodProvider` - for customizing how Kubernetes pods are discovered
2. `PodHealthChecker` - for customizing how pod health is checked
3. `PodConverter` - for customizing how Kubernetes pods are converted to PushpinServer objects

To provide your own implementation, simply define a bean in your Spring application:

```kotlin
@Configuration
class CustomKubernetesDiscoveryConfig {

    @Bean
    fun kubernetesPodProvider(): KubernetesPodProvider {
        return CustomKubernetesPodProvider()
    }
    
    @Bean
    fun podHealthChecker(): PodHealthChecker {
        return CustomPodHealthChecker()
    }
    
    @Bean
    fun podConverter(): PodConverter {
        return CustomPodConverter()
    }
}
```

## Required Kubernetes Permissions

The Kubernetes discovery mechanism requires the following RBAC permissions:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pushpin-discovery
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["services"]
  verbs: ["get", "list", "watch"]
```

### Using Service Accounts

For in-cluster configuration, the simplest way to provide these permissions is to attach a service account with the necessary RBAC permissions to the pods running your application.

## Pod Requirements

Kubernetes pods running Pushpin servers should:

1. Have appropriate labels matching your configuration (e.g., `app=pushpin`)
2. Be in the 'Running' phase
3. Have health status 'OK' if health checks are enabled
4. Be accessible from your application (network, service mesh, etc.)

## Example Setup

### 1. Label Kubernetes Pods

Ensure your Pushpin pods have appropriate labels:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pushpin
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pushpin
  template:
    metadata:
      labels:
        app: pushpin
    spec:
      containers:
      - name: pushpin
        image: fanout/pushpin:latest
        ports:
        - containerPort: 7999
        - containerPort: 5564
        - containerPort: 5560
```

### 2. Configure Discovery

In your `application.properties` or `application.yml`:

```properties
# Enable Pushpin discovery
pushpin.discovery.enabled=true

# Configure Kubernetes discovery
pushpin.discovery.kubernetes.enabled=true
pushpin.discovery.kubernetes.namespace=default
pushpin.discovery.kubernetes.labelSelector=app=pushpin
pushpin.discovery.kubernetes.refreshCacheSeconds=30
```

## Troubleshooting

If you're having issues with Kubernetes discovery:

1. Check that Kubernetes discovery is enabled (`pushpin.discovery.kubernetes.enabled=true`)
2. Verify your Kubernetes credentials and namespace configuration
3. Ensure pods have the expected labels
4. Check that pods are in the 'Running' phase and passing health checks
5. Verify network connectivity between your application and Pushpin pods
6. Check logs for error messages (set logging level to DEBUG for more details)
   ```properties
   logging.level.io.github.mpecan.pmt.discovery.kubernetes=DEBUG
   ```
7. For testing, you can create a local Kubernetes cluster with Minikube or Kind