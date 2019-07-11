package org.apache.sling.graalvm.osgi;

import org.apache.sling.engine.impl.SlingRequestProcessorWrapper;
import org.apache.sling.graalvm.sling.MockResourceProvider;
import org.apache.sling.graalvm.sling.MockServiceUserMapper;
import org.apache.sling.graalvm.sling.ResourceResolverFactoryService;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;

public class SlingContext {
    private static OsgiContext context;
    
    public static OsgiContext get() {
        if(context != null) {
            return context;
        }

        synchronized(SlingContext.class) {
            context = initialize();
        }

        return context;
    }

    /** This is where we wire the system, like the OSGi framework
     *  would do. As it seems hard to run that framework in a GraalVM
     *  environment for now, we wire things statically.
     */
    private static OsgiContext initialize() {
        final OsgiContext result = new OsgiContext();

        // This would be automatic in a JUnit environment
        result.registerInjectActivateService(new MockEventAdmin());

        // Our minimal resource provider
        final MockResourceProvider mrp = new MockResourceProvider();
        result.registerInjectActivateService(mrp);

        // SlingRequestProcessor
        result.registerInjectActivateService(new SlingRequestProcessorWrapper(result.bundleContext()));

        // ResourceResolver
        //result.registerInjectActivateService(new MockResourceResolver(mrp));
        result.registerInjectActivateService(new MockServiceUserMapper());
        result.registerInjectActivateService(new ResourceAccessSecurityTracker());
        final ResourceResolverFactoryActivator rrfa = null; // NATIVE new ResourceResolverFactoryActivator();
        // NATIVE result.registerInjectActivateService(rrfa);
        result.registerInjectActivateService(new ResourceResolverFactoryService(rrfa));

        return result;
    }
}