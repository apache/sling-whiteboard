package org.apache.sling.graalvm.sling;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderTracker;

public class MockResourceProviderTracker extends ResourceProviderTracker {

    @Override
    public ResourceProviderStorage getResourceProviderStorage() {
        final List<ResourceProviderHandler> handlers = new ArrayList<ResourceProviderHandler>();
        return new ResourceProviderStorage(handlers);
    }
}