package io.sskk.kubernetes.crd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.fabric8.junit.jupiter.api.KubernetesTest;
import io.fabric8.junit.jupiter.api.RequireK8sSupport;
import io.fabric8.junit.jupiter.api.RequireK8sVersionAtLeast;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;

@KubernetesTest
@RequireK8sSupport(Pod.class)
@RequireK8sVersionAtLeast(majorVersion = 1, minorVersion = 16)
public class CustomResourceTest {
    KubernetesClient kubernetesClient;

    @Test
    public void testGetApiVersion() {
        // // Given
        // Map<String, String> matchLabel = Collections.singletonMap("app", "list");
        // PodGroupService podGroupService = new PodGroupService(kubernetesClient, matchLabel);
        // // When
        // PodList podList = podGroupService.list();

        // // Then
        // assertNotNull(podList);
    }
}
