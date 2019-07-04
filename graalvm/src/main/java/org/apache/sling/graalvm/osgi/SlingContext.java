package org.apache.sling.graalvm.osgi;

import org.apache.sling.engine.impl.SlingRequestProcessorWrapper;
import org.apache.sling.graalvm.sling.MockResourceProvider;
import org.apache.sling.graalvm.sling.MockResourceResolver;
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
        final MockResourceProvider mrp = new MockResourceProvider();
        result.registerInjectActivateService(new MockResourceResolver(mrp));
        result.registerInjectActivateService(mrp);
        result.registerInjectActivateService(new SlingRequestProcessorWrapper(result.bundleContext()));
        return result;
    }
}