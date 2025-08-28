/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.slingservlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.exceptions.HttpStatusCodeException;
import org.apache.sling.microsling.helpers.servlets.SlingAllMethodsServlet;

/**
 * The default SlingServlet, used if no other SlingServlet wants to process the
 * current request.
 */
public class DefaultSlingServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain");

        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(req);
        final Resource  r = ctx.getResource();
        if (r == null) {
            throw new HttpStatusCodeException(404, "Resource not found: "
                + req.getPathInfo());
        }

        final Item data = r.getItem();
        if (data != null) {
            final PrintWriter pw = resp.getWriter();
            try {
                if (data.isNode()) {
                    dump(pw, r, (Node) data);
                } else {
                    dump(pw, r, (Property) data);
                }
            } catch (RepositoryException re) {
                throw new ServletException("Cannot dump contents of "
                    + ctx.getResource().getURI(), re);
            }
        } else {
            throw new HttpStatusCodeException(501,
                "Not implemented: resource " + ctx.getResource().getURI()
                    + " cannot be dumped by " + getClass().getSimpleName());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(req);
        String redirectPath = req.getPathInfo();

        Session s = null;
        try {
            s = ctx.getSession();
            Node current = (ctx.getResource() == null ? null : (Node)ctx.getResource().getItem());

            // Decide whether to create or update a node
            // TODO: this is a simplistic way of deciding, for now: if we have
            // no Resource or if the Node that it points to already has child nodes,
            // we create a new node. Else we update the current node.
            if(current == null || current.hasNodes()) {
                final String parentPath = (current == null ? req.getPathInfo() : current.getPath());
                final String newNodePath = parentPath + "/" + System.currentTimeMillis();
                current = deepCreateNode(s, newNodePath);
            }

            // Copy request parameters to node properties and save
            setPropertiesFromRequest(current, req);
            s.save();
            redirectPath = current.getPath();

        } catch (RepositoryException re) {
            throw new ServletException("Failed to modify content: "
                + re.getMessage(), re);

        } finally {
            try {
                if (s != null && s.hasPendingChanges()) {
                    s.refresh(false);
                }
            } catch (RepositoryException re) {
                // TODO: might want to log, but don't further care
            }
        }

        // redirect to the created node, so that it is displayed using a user-supplied extension
        String redirectExtension = req.getParameter("slingDisplayExtension");
        final String redirectUrl =
            req.getContextPath() + req.getServletPath() + redirectPath
            + (redirectExtension == null ? "" : "." + redirectExtension)
        ;
        resp.sendRedirect(redirectUrl);
    }

    /** Set node properties from current request (only handles Strings for now) */
    protected void setPropertiesFromRequest(Node n, HttpServletRequest req)
            throws RepositoryException {
        // TODO ignore sling-specific properties like slingDisplayExtension
        for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
            final String name = (String) e.nextElement();
            final String[] values = req.getParameterValues(name);
            if (values.length==1) {
            	n.setProperty(name, values[0]);
            } else {
            	n.setProperty(name, values);
            }
        }
    }

    /**
     * Deep creates a node, parent-padding with nt:unstructured nodes
     *
     * @param path absolute path to node that needs to be deep-created
     */
    protected Node deepCreateNode(Session s, String path)
            throws RepositoryException {
        String[] pathelems = path.substring(1).split("/");
        int i = 0;
        String mypath = "";
        Node parent = s.getRootNode();
        while (i < pathelems.length) {
            String name = pathelems[i];
            mypath += "/" + name;
            if (!s.itemExists(mypath)) {
                parent.addNode(name);
            }
            parent = (Node) s.getItem(mypath);
            i++;
        }
        return (parent);
    }

    protected void dump(PrintWriter pw, Resource r, Node n) throws RepositoryException {
        pw.println("** Node dumped by " + getClass().getSimpleName() + "**");
        pw.println("Node path:" + n.getPath());
        pw.println("Resource metadata: " + r.getMetadata());

        pw.println("\n** Node properties **");
        for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
            final Property p = pi.nextProperty();
            printPropertyValue(pw, p);
        }
    }

    protected void dump(PrintWriter pw, Resource r, Property p) throws RepositoryException {
        pw.println("** Property dumped by " + getClass().getSimpleName() + "**");
        pw.println("Property path:" + p.getPath());
        pw.println("Resource metadata: " + r.getMetadata());

        printPropertyValue(pw, p);
    }

    protected void printPropertyValue(PrintWriter pw, Property p)
            throws RepositoryException {

        pw.print(p.getName() + ": ");

        if (p.getDefinition().isMultiple()) {
            Value[] values = p.getValues();
            pw.print('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(values[i].getString());
            }
            pw.println(']');
        } else {
            pw.println(p.getValue().getString());
        }
    }

}
