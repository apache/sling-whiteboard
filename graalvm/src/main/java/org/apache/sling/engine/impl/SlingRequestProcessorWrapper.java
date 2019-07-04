package org.apache.sling.engine.impl;

import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.engine.impl.SlingRequestProcessorImpl;
import org.apache.sling.graalvm.sling.MockErrorHandler;
import org.apache.sling.graalvm.sling.MockFilterManager;
import org.apache.sling.graalvm.sling.MockServletResolver;
import org.osgi.service.component.annotations.Component;

/** TODO hack: using the engine.impl package to access package private methods... */
@Component(service=SlingRequestProcessor.class)
public class SlingRequestProcessorWrapper extends SlingRequestProcessorImpl {
    public SlingRequestProcessorWrapper() {
        this.setServletResolver(new MockServletResolver());
        this.setErrorHandler(new MockErrorHandler());
        this.setFilterManager(new MockFilterManager());
    }
}