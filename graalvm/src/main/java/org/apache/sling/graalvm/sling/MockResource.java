package org.apache.sling.graalvm.sling;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MockResource extends AbstractResource {

    private final String path;
    private final ResourceResolver resolver;
    private final ResourceMetadata metadata;

    public MockResource(ResourceResolver resolver, String path) {
        this.resolver = resolver;
        this.path = path;
        this.metadata = new ResourceMetadata();
        metadata.put(ResourceMetadata.RESOLUTION_PATH, path);
        metadata.put(ResourceMetadata.RESOLUTION_PATH_INFO, path);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return "mock/resource";
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public Resource getParent() {
        return null;
    }
}