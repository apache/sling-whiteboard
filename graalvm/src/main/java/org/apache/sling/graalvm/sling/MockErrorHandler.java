package org.apache.sling.graalvm.sling;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.servlets.ErrorHandler;

public class MockErrorHandler implements ErrorHandler {

    @Override
    public void handleError(int status, String message, SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {
        response.getWriter().write(getClass().getSimpleName() + ':' + status + ':' + message);
    }

    @Override
    public void handleError(Throwable throwable, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        response.getWriter().write(getClass().getSimpleName() + ':' + throwable.toString());
    }
    
}