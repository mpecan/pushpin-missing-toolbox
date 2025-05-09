package io.github.mpecan.pmt.discovery.kubernetes.pods

import io.github.mpecan.pmt.discovery.kubernetes.KubernetesDiscoveryProperties
import io.github.mpecan.pmt.discovery.kubernetes.clients.KubernetesClientProvider
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKubernetesPodProviderTest {

    private val clientProvider: KubernetesClientProvider = mock()
    private val coreApi: CoreV1Api = mock()
    private val apiListNamespacedPodRequest: CoreV1Api.APIlistNamespacedPodRequest = mock()
    private val apiListPodForAllNamespacesRequest: CoreV1Api.APIlistPodForAllNamespacesRequest = mock()
    private val apiReadNamespacedServiceRequest: CoreV1Api.APIreadNamespacedServiceRequest = mock()

    private val properties = KubernetesDiscoveryProperties(
        enabled = true,
        namespace = "test-namespace",
        labelSelector = "app=pushpin",
        fieldSelector = "status.phase=Running",
    )

    private val podProvider = DefaultKubernetesPodProvider(clientProvider)

    @Test
    fun `should get pods directly with namespace specified`() {
        // Given
        val podList = V1PodList().items(
            listOf(
                createPod("pod-1", "192.168.1.1"),
                createPod("pod-2", "192.168.1.2"),
            ),
        )

        whenever(clientProvider.getCoreApi(properties)).thenReturn(coreApi)
        whenever(coreApi.listNamespacedPod(eq("test-namespace"))).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.fieldSelector(properties.fieldSelector)).thenReturn(
            apiListNamespacedPodRequest,
        )
        whenever(apiListNamespacedPodRequest.labelSelector(properties.labelSelector)).thenReturn(
            apiListNamespacedPodRequest,
        )
        whenever(apiListNamespacedPodRequest.execute()).thenReturn(podList)

        // When
        val result = podProvider.getPods(properties)

        // Then
        assertEquals(2, result.size)
        assertEquals("pod-1", result[0].metadata?.name)
        assertEquals("pod-2", result[1].metadata?.name)

        verify(clientProvider).getCoreApi(properties)
        verify(coreApi).listNamespacedPod("test-namespace")
        verify(apiListNamespacedPodRequest).fieldSelector(properties.fieldSelector)
        verify(apiListNamespacedPodRequest).labelSelector(properties.labelSelector)
        verify(apiListNamespacedPodRequest).execute()
    }

    @Test
    fun `should get pods directly from all namespaces when namespace not specified`() {
        // Given
        val propertiesWithoutNamespace = KubernetesDiscoveryProperties(
            enabled = true,
            namespace = null,
            labelSelector = "app=pushpin",
            fieldSelector = "status.phase=Running",
        )

        val podList = V1PodList().items(
            listOf(
                createPod("pod-1", "192.168.1.1"),
                createPod("pod-2", "192.168.1.2"),
            ),
        )

        whenever(clientProvider.getCoreApi(propertiesWithoutNamespace)).thenReturn(coreApi)
        whenever(coreApi.listPodForAllNamespaces()).thenReturn(apiListPodForAllNamespacesRequest)
        whenever(apiListPodForAllNamespacesRequest.fieldSelector(propertiesWithoutNamespace.fieldSelector)).thenReturn(
            apiListPodForAllNamespacesRequest,
        )
        whenever(apiListPodForAllNamespacesRequest.labelSelector(propertiesWithoutNamespace.labelSelector)).thenReturn(
            apiListPodForAllNamespacesRequest,
        )
        whenever(apiListPodForAllNamespacesRequest.execute()).thenReturn(podList)

        // When
        val result = podProvider.getPods(propertiesWithoutNamespace)

        // Then
        assertEquals(2, result.size)
        assertEquals("pod-1", result[0].metadata?.name)
        assertEquals("pod-2", result[1].metadata?.name)

        verify(clientProvider).getCoreApi(propertiesWithoutNamespace)
        verify(coreApi).listPodForAllNamespaces()
        verify(apiListPodForAllNamespacesRequest).fieldSelector(propertiesWithoutNamespace.fieldSelector)
        verify(apiListPodForAllNamespacesRequest).labelSelector(propertiesWithoutNamespace.labelSelector)
        verify(apiListPodForAllNamespacesRequest).execute()
    }

    @Test
    fun `should get pods from service using service selector`() {
        // Given
        val serviceProperties = KubernetesDiscoveryProperties(
            enabled = true,
            namespace = "test-namespace",
            useService = true,
            serviceName = "pushpin-service",
        )

        val selector = mapOf("app" to "pushpin", "environment" to "test")
        val serviceSpec = V1ServiceSpec().selector(selector)
        val service = V1Service().spec(serviceSpec)

        val podList = V1PodList().items(
            listOf(
                createPod("pod-1", "192.168.1.1"),
                createPod("pod-2", "192.168.1.2"),
            ),
        )

        whenever(clientProvider.getCoreApi(serviceProperties)).thenReturn(coreApi)
        whenever(coreApi.readNamespacedService(eq("pushpin-service"), eq("test-namespace"))).thenReturn(
            apiReadNamespacedServiceRequest,
        )
        whenever(apiReadNamespacedServiceRequest.execute()).thenReturn(service)

        whenever(coreApi.listNamespacedPod(eq("test-namespace"))).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.fieldSelector(any())).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.labelSelector(eq("app=pushpin,environment=test"))).thenReturn(
            apiListNamespacedPodRequest,
        )
        whenever(apiListNamespacedPodRequest.execute()).thenReturn(podList)

        // When
        val result = podProvider.getPods(serviceProperties)

        // Then
        assertEquals(2, result.size)
        assertEquals("pod-1", result[0].metadata?.name)
        assertEquals("pod-2", result[1].metadata?.name)

        verify(clientProvider).getCoreApi(serviceProperties)
        verify(coreApi).readNamespacedService("pushpin-service", "test-namespace")
        verify(apiReadNamespacedServiceRequest).execute()
        verify(coreApi).listNamespacedPod("test-namespace")
        verify(apiListNamespacedPodRequest).labelSelector("app=pushpin,environment=test")
        verify(apiListNamespacedPodRequest).execute()
    }

    @Test
    fun `should handle service without selectors`() {
        // Given
        val serviceProperties = KubernetesDiscoveryProperties(
            enabled = true,
            namespace = "test-namespace",
            useService = true,
            serviceName = "pushpin-service",
        )

        val serviceSpec = V1ServiceSpec() // No selectors
        val service = V1Service().spec(serviceSpec)

        whenever(clientProvider.getCoreApi(serviceProperties)).thenReturn(coreApi)
        whenever(coreApi.readNamespacedService(eq("pushpin-service"), eq("test-namespace"))).thenReturn(
            apiReadNamespacedServiceRequest,
        )
        whenever(apiReadNamespacedServiceRequest.execute()).thenReturn(service)

        // When
        val result = podProvider.getPods(serviceProperties)

        // Then
        assertTrue(result.isEmpty())

        verify(clientProvider).getCoreApi(serviceProperties)
        verify(coreApi).readNamespacedService("pushpin-service", "test-namespace")
        verify(apiReadNamespacedServiceRequest).execute()
        verify(coreApi, never()).listNamespacedPod(any())
    }

    @Test
    fun `should handle API exception when getting pods`() {
        // Given
        whenever(clientProvider.getCoreApi(properties)).thenReturn(coreApi)
        whenever(coreApi.listNamespacedPod(any())).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.fieldSelector(any())).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.labelSelector(any())).thenReturn(apiListNamespacedPodRequest)
        whenever(apiListNamespacedPodRequest.execute()).thenThrow(ApiException("Test exception"))

        // When
        val result = podProvider.getPods(properties)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle API exception when getting service`() {
        // Given
        val serviceProperties = KubernetesDiscoveryProperties(
            enabled = true,
            namespace = "test-namespace",
            useService = true,
            serviceName = "pushpin-service",
        )

        whenever(clientProvider.getCoreApi(serviceProperties)).thenReturn(coreApi)
        whenever(coreApi.readNamespacedService(any(), any())).thenReturn(apiReadNamespacedServiceRequest)
        whenever(apiReadNamespacedServiceRequest.execute()).thenThrow(ApiException("Test exception"))

        // When
        val result = podProvider.getPods(serviceProperties)

        // Then
        assertTrue(result.isEmpty())
    }

    private fun createPod(name: String, podIp: String): V1Pod {
        return V1Pod()
            .metadata(V1ObjectMeta().name(name))
            .status(V1PodStatus().podIP(podIp))
    }
}
