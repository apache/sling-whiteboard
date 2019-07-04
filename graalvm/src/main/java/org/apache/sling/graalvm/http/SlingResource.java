package org.apache.sling.graalvm.http;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.sling.graalvm.osgi.SlingContext;
import org.apache.sling.graalvm.sling.MockServletResolver;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.mockito.Mockito;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

@Path("/sling/{resourcePath: [^/][a-zA-Z/_0-9\\.]*}")
@Produces(MediaType.APPLICATION_JSON)
public class SlingResource {

    @Context
    private HttpServletRequest request;

    @GET
    public Response sling(@PathParam("resourcePath") String resourcePath) throws IOException {
        final SlingRequestProcessor p = SlingContext.get().getService(SlingRequestProcessor.class);
        assert (p != null);
        final ResourceResolver resolver = SlingContext.get().getService(ResourceResolver.class);
        assert (resolver != null);

        final StringWriter sw = new StringWriter();
        final HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        
        try {
            p.processRequest(request, resp, resolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Resource r = (Resource)request.getAttribute(MockServletResolver.RESOURCE_ATTR);
        return Response.ok(r).build();
    }
}