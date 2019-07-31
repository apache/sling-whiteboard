package org.apache.sling.graalvm.sling;

import org.apache.sling.engine.impl.SlingMainServlet;
import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.osgi.framework.BundleContext;

public class MockFilterManager extends ServletFilterManager {
    public MockFilterManager(BundleContext bc) {
        super(
            bc, 
            new SlingServletContext(bc, new SlingMainServlet())
        );
    }
}