package org.apache.sling.graalvm.sling;

import org.apache.sling.engine.impl.filter.ServletFilterManager;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class MockFilterManager extends ServletFilterManager {
    public MockFilterManager() {
        super(Mockito.mock(BundleContext.class), Mockito.mock(SlingServletContext.class));
    }
}