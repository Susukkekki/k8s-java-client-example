package io.sskk.kubernetes.crd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.bouncycastle.math.raw.Mod;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

public class InferenceServiceTest {

    final static String VERSION = "v1beta1";
    final static String GROUP = "serving.kserve.io";

    @Version(VERSION)    
    @Group(GROUP)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InferenceService extends io.fabric8.kubernetes.client.CustomResource<InferenceServiceSpec, Void> implements Namespaced {
    }

    // @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InferenceServiceSpec extends KubernetesList {
        private Predictor predictor;

        public Predictor getPredictor() {
            return predictor;
        }

        public void setPredictor(Predictor predictor) {
            this.predictor = predictor;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Predictor {
        private Model model;

        public Model getModel() {
            return model;
        }

        public void setModel(Model model) {
            this.model = model;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Model {
        private ModelFormat modelFormat;

        private String name;

        private String storageUri;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }        

        public String getStorageUri() {
            return storageUri;
        }

        public void setStorageUri(String storageUri) {
            this.storageUri = storageUri;
        }

        public ModelFormat getModelFormat() {
            return modelFormat;
        }

        public void setModelFormat(ModelFormat modelFormat) {
            this.modelFormat = modelFormat;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelFormat {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }


    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typed-api
     */
    @Test
    public void testListTypedInferenceService() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceclient = client.resources(InferenceService.class);
        KubernetesResourceList<InferenceService> list = inferenceServiceclient.inNamespace("default").list();

        assertTrue(list.getItems().size() >= 1);

        assertEquals("sklearn", list.getItems().get(0).getSpec().getPredictor().getModel().getModelFormat().getName());
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typeless-api
     */
    @Test
    public void testListTypessInferenceService() {
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
            .withVersion(VERSION)
            .withGroup(GROUP)
            .withPlural("inferenceservices")
            .withNamespaced(true)
            .build();

        KubernetesClient client = new KubernetesClientBuilder().build();
        GenericKubernetesResourceList list = client.genericKubernetesResources(resourceDefinitionContext).inNamespace("default").list();
        
        assertTrue(list.getItems().size() >= 1);

        Map<String, Object> spec = (Map<String, Object>)list.getItems().get(0).getAdditionalProperties().get("spec");
        Map<String, Object> predictor = (Map<String, Object>)spec.get("predictor");
        Map<String, Object> model = (Map<String, Object>)predictor.get("model");
        Map<String, Object> modelFormat = (Map<String, Object>)model.get("modelFormat");
        String modelFormatName = (String)modelFormat.get("name");

        assertEquals("sklearn", modelFormatName);

        /*        
        list.getItems().get(0).getMetadata().getAnnotations().toString()        
            "{a=b, kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"serving.kserve.io/v1beta1","kind":"InferenceService","metadata":{"annotations":{},"name":"sklearn-iris","namespace":"default"},"spec":{"predictor":{"model":{"modelFormat":{"name":"sklearn"},"storageUri":"gs://kfserving-examples/models/sklearn/1.0/model"}}}}    
         */
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typed-api
     */
    @Test
    public void testCreateTypedInferenceService() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceClient = client.resources(InferenceService.class);

        InferenceService inferenceService = new InferenceService();
        InferenceServiceSpec inferenceServiceSpec = new InferenceServiceSpec();
        inferenceService.getMetadata().setName("test");
        inferenceService.setSpec(inferenceServiceSpec);

        Predictor predictor = new Predictor();
        Model model = new Model();
        ModelFormat modelFormat = new ModelFormat();
        modelFormat.setName("sklearn");
        model.setStorageUri("gs://kfserving-examples/models/sklearn/1.0/model");
        model.setModelFormat(modelFormat);
        predictor.setModel(model);
        inferenceService.getSpec().setPredictor(predictor);        

        InferenceService createdInferenceService = inferenceServiceClient.inNamespace("default").resource(inferenceService).create();
        assertEquals("test", createdInferenceService.getMetadata().getName());


        KubernetesResourceList<InferenceService> list = inferenceServiceClient.inNamespace("default").list();
        assertTrue(list.getItems().size() >= 2);
        assertEquals("sklearn", list.getItems().get(0).getSpec().getPredictor().getModel().getModelFormat().getName());
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typeless-api
     */
    @Test
    public void testCreateTypessInferenceServiceWithJson() {
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
            .withVersion(VERSION)
            .withGroup(GROUP)
            .withPlural("inferenceservices")
            .withNamespaced(true)
            .build();

        KubernetesClient client = new KubernetesClientBuilder().build();

        String rawJson = ""
            .concat("{")
            .concat("  \"apiVersion\": \"serving.kserve.io/v1beta1\",")
            .concat("  \"kind\": \"InferenceService\",")
            .concat("  \"metadata\": {")
            .concat("    \"name\": \"test-2\"")
            .concat("  },")
            .concat("  \"spec\": {")
            .concat("    \"predictor\": {")
            .concat("      \"model\": {")
            .concat("        \"modelFormat\": {")
            .concat("          \"name\": \"sklearn\"")
            .concat("        },")
            .concat("        \"storageUri\": \"gs://kfserving-examples/models/sklearn/1.0/model\"")
            .concat("      }")
            .concat("    }")
            .concat("  }")
            .concat("}");
        
        GenericKubernetesResource object = client.genericKubernetesResources(resourceDefinitionContext)
            .inNamespace("default")    
            .load(new ByteArrayInputStream(rawJson.getBytes())).create();

        GenericKubernetesResourceList list = client.genericKubernetesResources(resourceDefinitionContext).inNamespace("default").list();
        
        
        assertTrue(list.getItems().size() >= 3);

        Map<String, Object> spec = (Map<String, Object>)list.getItems().get(0).getAdditionalProperties().get("spec");
        Map<String, Object> predictor = (Map<String, Object>)spec.get("predictor");
        Map<String, Object> model = (Map<String, Object>)predictor.get("model");
        Map<String, Object> modelFormat = (Map<String, Object>)model.get("modelFormat");
        String modelFormatName = (String)modelFormat.get("name");

        assertEquals("sklearn", modelFormatName);

        /*        
        list.getItems().get(0).getMetadata().getAnnotations().toString()        
            "{a=b, kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"serving.kserve.io/v1beta1","kind":"InferenceService","metadata":{"annotations":{},"name":"sklearn-iris","namespace":"default"},"spec":{"predictor":{"model":{"modelFormat":{"name":"sklearn"},"storageUri":"gs://kfserving-examples/models/sklearn/1.0/model"}}}}    
         */
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typeless-api
     */
    @Test
    public void testCreateTypessInferenceServiceWithYaml() {
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
            .withVersion(VERSION)
            .withGroup(GROUP)
            .withPlural("inferenceservices")
            .withNamespaced(true)
            .build();

        KubernetesClient client = new KubernetesClientBuilder().build();

        String rawYaml = ""
            .concat("apiVersion: \"serving.kserve.io/v1beta1\"\n")
            .concat("kind: \"InferenceService\"\n")
            .concat("metadata:\n")
            .concat("  name: \"test-3\"\n")
            .concat("spec:\n")
            .concat("  predictor:\n")
            .concat("    model:\n")
            .concat("      modelFormat:\n")
            .concat("        name: sklearn\n")
            .concat("      storageUri: \"gs://kfserving-examples/models/sklearn/1.0/model\"");        
        
        GenericKubernetesResource object = client.genericKubernetesResources(resourceDefinitionContext)
            .inNamespace("default")    
            .load(new ByteArrayInputStream(rawYaml.getBytes())).create();

        GenericKubernetesResourceList list = client.genericKubernetesResources(resourceDefinitionContext).inNamespace("default").list();
                
        assertTrue(list.getItems().size() >= 4);

        Map<String, Object> spec = (Map<String, Object>)list.getItems().get(0).getAdditionalProperties().get("spec");
        Map<String, Object> predictor = (Map<String, Object>)spec.get("predictor");
        Map<String, Object> model = (Map<String, Object>)predictor.get("model");
        Map<String, Object> modelFormat = (Map<String, Object>)model.get("modelFormat");
        String modelFormatName = (String)modelFormat.get("name");

        assertEquals("sklearn", modelFormatName);

        /*        
        list.getItems().get(0).getMetadata().getAnnotations().toString()        
            "{a=b, kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"serving.kserve.io/v1beta1","kind":"InferenceService","metadata":{"annotations":{},"name":"sklearn-iris","namespace":"default"},"spec":{"predictor":{"model":{"modelFormat":{"name":"sklearn"},"storageUri":"gs://kfserving-examples/models/sklearn/1.0/model"}}}}    
         */
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typed-api
     */
    @Test
    public void testPatchTypedInferenceService() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceClient = client.resources(InferenceService.class);

        String name = "test";
        String storageUri = "gs://kfserving-examples/models/sklearn/1.0/model2";

        InferenceService inferenceService = new InferenceService();
        InferenceServiceSpec inferenceServiceSpec = new InferenceServiceSpec();
        inferenceService.getMetadata().setName(name);
        inferenceService.setSpec(inferenceServiceSpec);

        Predictor predictor = new Predictor();
        Model model = new Model();
        // ModelFormat modelFormat = new ModelFormat();
        // modelFormat.setName("sklearn");
        model.setStorageUri(storageUri);
        // model.setModelFormat(modelFormat);
        predictor.setModel(model);
        inferenceService.getSpec().setPredictor(predictor);        

        InferenceService createdInferenceService = inferenceServiceClient.inNamespace("default").resource(inferenceService).patch();
        assertEquals(name, createdInferenceService.getMetadata().getName());
        assertEquals(storageUri, createdInferenceService.getSpec().getPredictor().getModel().getStorageUri());

        // KubernetesResourceList<InferenceService> list = inferenceServiceClient.inNamespace("default").list();
        // assertTrue(list.getItems().size() >= 2);
        // assertEquals("storageUri", list.getItems().get(0).getSpec().getPredictor().getModel().getModelFormat().getName());
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typeless-api
     */
    @Test
    public void testPathTypessInferenceServiceWithYaml() {
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
            .withVersion(VERSION)
            .withGroup(GROUP)
            .withPlural("inferenceservices")
            .withNamespaced(true)
            .build();

        KubernetesClient client = new KubernetesClientBuilder().build();

        String name = "test";
        String storageUri = "gs://kfserving-examples/models/sklearn/1.0/model3";

        String rawYaml = ""
                          .concat("apiVersion: \"serving.kserve.io/v1beta1\"\n")
                          .concat("kind: \"InferenceService\"\n")
                          .concat("metadata:\n")
            .concat(String.format("  name: \"%s\"\n", name))
                          .concat("spec:\n")
                          .concat("  predictor:\n")
                          .concat("    model:\n")
                          .concat("      modelFormat:\n")
                          .concat("        name: sklearn\n")
            .concat(String.format("      storageUri: \"%s\"", storageUri));        
        
        GenericKubernetesResource object = client.genericKubernetesResources(resourceDefinitionContext)
            .inNamespace("default")    
            .load(new ByteArrayInputStream(rawYaml.getBytes())).patch();        

        Map<String, Object> spec = (Map<String, Object>)object.getAdditionalProperties().get("spec");
        Map<String, Object> predictor = (Map<String, Object>)spec.get("predictor");
        Map<String, Object> model = (Map<String, Object>)predictor.get("model");
        String returnedStorageUri = (String)model.get("storageUri");
        Map<String, Object> modelFormat = (Map<String, Object>)model.get("modelFormat");
        String modelFormatName = (String)modelFormat.get("name");

        assertEquals(name, object.getMetadata().getName());
        assertEquals(storageUri, returnedStorageUri);

        /*        
        list.getItems().get(0).getMetadata().getAnnotations().toString()        
            "{a=b, kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"serving.kserve.io/v1beta1","kind":"InferenceService","metadata":{"annotations":{},"name":"sklearn-iris","namespace":"default"},"spec":{"predictor":{"model":{"modelFormat":{"name":"sklearn"},"storageUri":"gs://kfserving-examples/models/sklearn/1.0/model"}}}}    
         */
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typed-api
     */
    @Test
    public void testDeleteTypedInferenceService() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceClient = client.resources(InferenceService.class);

        String name = "test";

        InferenceService inferenceService = new InferenceService();
        inferenceService.getMetadata().setName(name);        

        inferenceServiceClient.inNamespace("default").resource(inferenceService).delete();                
    }

    /**
     * @see https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md#resource-typeless-api
     */
    @Test
    public void testDeleteTypessInferenceServiceWithYaml() {
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
            .withVersion(VERSION)
            .withGroup(GROUP)
            .withPlural("inferenceservices")
            .withNamespaced(true)
            .build();

        KubernetesClient client = new KubernetesClientBuilder().build();

        String name = "test-2";

        String rawYaml = ""
                          .concat("apiVersion: \"serving.kserve.io/v1beta1\"\n")
                          .concat("kind: \"InferenceService\"\n")
                          .concat("metadata:\n")
            .concat(String.format("  name: \"%s\"\n", name));            
        
        client.genericKubernetesResources(resourceDefinitionContext)
            .inNamespace("default")    
            .load(new ByteArrayInputStream(rawYaml.getBytes())).delete();        
    }
}
