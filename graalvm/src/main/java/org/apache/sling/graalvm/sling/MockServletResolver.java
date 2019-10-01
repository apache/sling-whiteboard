package org.apache.sling.graalvm.sling;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

public class MockServletResolver implements ServletResolver {

    public static final String RESOURCE_ATTR = MockServletResolver.class.getName();

    static class DummyServlet extends SlingSafeMethodsServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
            request.setAttribute(RESOURCE_ATTR, request.getResource());
        }
    }

    @Override
    public Servlet resolveServlet(SlingHttpServletRequest request) {
        return new DummyServlet();
    }

    @Override
    public Servlet resolveServlet(Resource resource, String scriptName) {
        return new DummyServlet();
    }

    @Override
    public Servlet resolveServlet(ResourceResolver resolver, String scriptName) {
        return new DummyServlet();
    }
}