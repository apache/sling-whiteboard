package org.apache.sling.graalvm.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SlingContextTest {

    private static ResourceResolver resolver;
    
    @BeforeAll
    public static void setup() throws LoginException {
        final ResourceResolverFactory fac = SlingContext.get().getService(ResourceResolverFactory.class);
        assertNotNull("Expecting a ResourceResolverFactory", fac);
        resolver = fac.getResourceResolver(new HashMap<String, Object>());
        assertNotNull("Expecting a ResourceResolver", resolver);
    }

    @Test
    public void resolveResource() {
        final String path = "/sling/chouc/route";
        final Resource r = resolver.resolve(path);
        assertNotNull("Expecting Resource at " + path, r);
        assertEquals("sling:nonexisting", r.getResourceType());
    }
}