package io.sskk.kubernetes.crd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.RevisionList;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class KnativeClientTest {
    @Test
    void testKnativeClient() {
        KnativeClient client = new KubernetesClientBuilder().build().adapt(KnativeClient.class);
        RevisionList revisionList = client.revisions().inNamespace("default").list();
        
        assertTrue(revisionList.getItems().size() > 0);
    }
}
