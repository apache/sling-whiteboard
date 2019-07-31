package org.apache.sling.graalvm.sling;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;

@Component(service=ResourceProvider.class)
public class MockResourceProvider extends ResourceProvider<MockResource> {

    private static final String MAGIC = "chouc";

    @Override
    public Resource getResource(ResolveContext<MockResource> ctx, String path, ResourceContext resourceContext,
            Resource parent) {
        return path.contains(MAGIC) ? new MockResource(null, path) : null;
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<MockResource> ctx, Resource parent) {
        return null;
    }
   
}