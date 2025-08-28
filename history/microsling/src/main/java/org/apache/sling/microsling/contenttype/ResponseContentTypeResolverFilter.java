package org.apache.sling.microsling.contenttype;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.helpers.AbstractFilter;
import org.apache.sling.microsling.request.MicroslingRequestContext;

public class ResponseContentTypeResolverFilter extends AbstractFilter {

    @Override
    protected void init() {
        // no further initialization
    }

    public void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        final MicroslingRequestContext ctx = (MicroslingRequestContext)SlingRequestContext.getFromRequest(request);

        String file = "dummy." + ctx.getRequestPathInfo().getExtension();
        final String contentType = getFilterConfig().getServletContext().getMimeType(file);
        if (contentType != null) {
            // Need this info in the SlingRequestContext,
            // for the SlingScriptResolver for example
            ctx.setResponseContentType(contentType);
        }

        filterChain.doFilter(request, response);

    }

}
