package org.apache.sling.graalvm.sling;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.resourceresolver.impl.CommonResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;
import org.osgi.service.component.annotations.Component;

@Component(service=ResourceResolverFactory.class)
public class ResourceResolverFactoryService extends CommonResourceResolverFactoryImpl
        implements ResourceResolverFactory {

    public ResourceResolverFactoryService(ResourceResolverFactoryActivator activator) {
        super(activator);
    }

    @Override
    public ResourceProviderTracker getResourceProviderTracker() {
        return new MockResourceProviderTracker();
    }

}