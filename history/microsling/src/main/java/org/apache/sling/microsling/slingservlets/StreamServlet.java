/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.microsling.slingservlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.exceptions.HttpStatusCodeException;
import org.apache.sling.microsling.helpers.constants.HttpConstants;
import org.apache.sling.microsling.helpers.servlets.SlingSafeMethodsServlet;

/**
 * The <code>StreamServlet</code> handles requests for nodes which may just be
 * streamed out to the response. If the requested JCR Item is an
 * <em>nt:file</em> whose <em>jcr:content</em> child node is of type
 * <em>nt:resource</em>, the response content type, last modification time and
 * charcter encoding are set according to the resource node. In addition if
 * the <em>If-Modified-Since</em> header is set, the resource will only be
 * spooled if the last modification time is later than the header. Otherwise
 * a 304 (Not Modified) status code is sent.
 * <p>
 * If the requested item is not an <em>nt:file</em>/<em>nt:resource</em> tuple,
 * the item is just resolved by following the primary item trail according to
 * the algorithm
 * <pre>
 *     while (item.isNode) {
 *         item = ((Node) item).getPrimaryItem();
 *     }
 * </pre>
 * Until a property is found or the primary item is either not defined or not
 * existing in which case an exception is thrown and the request fails with
 * a 404 (Not Found) status.
 */
public class StreamServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(request);


        try {
            Item item = ctx.getResource().getItem();

            // otherwise handle nt:file/nt:resource specially
            Node node = (Node) item;
            if (node.isNodeType("nt:file")) {
                Node content = node.getNode("jcr:content");
                if (content.isNodeType("nt:resource")) {

                    // check for if last modified
                    long ifModified = request.getDateHeader(HttpConstants.HEADER_IF_MODIFIED_SINCE);
                    long lastModified = getLastModified(content);
                    if (ifModified < 0 || lastModified > ifModified) {

                        String contentType = getMimeType(content);
                        if (contentType == null) {
                            contentType = ctx.getPreferredResponseContentType();
                        }

                        spool(response,
                            content.getProperty(JcrConstants.JCR_DATA),
                            contentType, getEncoding(content),
                            getLastModified(content));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }

                    return;
                }
            }

            // just spool, the property to which the item resolves through
            // the primary item trail
            // the item is a property, spool and forget
            spool(response, findDataProperty(item), null, null, -1);

        } catch (RepositoryException re) {
            throw new HttpStatusCodeException(500, "RepositoryException in StreamServlet.doGet(): " + re.getMessage());
        }
    }

    /**
     * Spool the property value to the response setting the content type,
     * character set, last modification data and content length header
     */
    private void spool(HttpServletResponse response, Property prop,
            String mimeType, String encoding, long lastModified)
            throws RepositoryException, IOException {

        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }

        if (lastModified > 0) {
            response.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, lastModified);
        }

        // only set the content length if the property is a binary
        if (prop.getType() == PropertyType.BINARY) {
            response.setContentLength((int) prop.getLength());
        }

        InputStream ins = prop.getStream();
        OutputStream out = null;
        try {
            ins = prop.getStream();
            out = response.getOutputStream();

            byte[] buf = new byte[2048];
            int num;
            while ((num = ins.read(buf)) >= 0) {
                out.write(buf, 0, num);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /** Find the Property that contains the data to spool, under parent */ 
    private Property findDataProperty(final Item parent) throws RepositoryException, HttpStatusCodeException {
        Property result = null;
        
        // Following the path of primary items until we find a property
        // should provide us with the file data of the parent
        try {
            Item item = parent;
            while(item!=null && item.isNode()) {
                item = ((Node) item).getPrimaryItem();
            }
            result = (Property)item;
        } catch(ItemNotFoundException ignored) {
            // TODO: for now we use an alternate method if this fails,
            // there might be a better way (see jackrabbit WebDAV server code?)
        }
        
        if(result==null && parent.isNode()) {
            // primary path didn't work, try the "usual" path to the data Property
            try {
                final Node parentNode = (Node)parent;
                result = parentNode.getNode("jcr:content").getProperty("jcr:data");
            } catch(ItemNotFoundException e) {
                throw new HttpStatusCodeException(404,parent.getPath() + "/jcr:content" + "/jcr:data");
            }
        }
        
        if(result==null) {
            throw new HttpStatusCodeException(500, "Unable to find data property for parent item " + parent.getPath());
        }
        
        return result;
    }

    /** return the jcr:lastModified property value or null if property is missing */
    private long getLastModified(Node resourceNode) throws RepositoryException {
        Property lastModifiedProp = getProperty(resourceNode,
            JcrConstants.JCR_LASTMODIFIED);
        return (lastModifiedProp != null) ? lastModifiedProp.getLong() : -1;
    }

    /** return the jcr:mimeType property value or null if property is missing */
    private String getMimeType(Node resourceNode) throws RepositoryException {
        Property mimeTypeProp = getProperty(resourceNode,
            JcrConstants.JCR_MIMETYPE);
        return (mimeTypeProp != null) ? mimeTypeProp.getString() : null;
    }

    /** return the jcr:encoding property value or null if property is missing */
    private String getEncoding(Node resourceNode) throws RepositoryException {
        Property encodingProp = getProperty(resourceNode,
            JcrConstants.JCR_ENCODING);
        return (encodingProp != null) ? encodingProp.getString() : null;
    }

    /** Return the named property or null if not existing or node is null */
    private Property getProperty(Node node, String relPath)
            throws RepositoryException {
        if (node != null && node.hasProperty(relPath)) {
            return node.getProperty(relPath);
        }

        return null;
    }
}
