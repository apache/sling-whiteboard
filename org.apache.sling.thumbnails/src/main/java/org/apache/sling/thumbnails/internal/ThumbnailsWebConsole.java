/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.thumbnails.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformer;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Simple web console plugin for listing out the registered thumbnail providers
 * and transformation handler
 */
@Component(property = { Constants.SERVICE_DESCRIPTION + "=Web Console Plugin for Apache Sling Thumbnails",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        WebConsoleConstants.PLUGIN_LABEL + "=" + ThumbnailsWebConsole.CONSOLE_LABEL,
        WebConsoleConstants.PLUGIN_TITLE + "=" + ThumbnailsWebConsole.CONSOLE_TITLE,
        WebConsoleConstants.CONFIG_PRINTER_MODES + "=always",
        WebConsoleConstants.PLUGIN_CATEGORY + "=Status" }, service = { Servlet.class })
public class ThumbnailsWebConsole extends AbstractWebConsolePlugin {

    private static final long serialVersionUID = 4819043498961127418L;
    public static final String CONSOLE_LABEL = "thumbnails";
    public static final String CONSOLE_TITLE = "Sling Thumbnails";

    private final Transformer transformer;
    private final ThumbnailSupport thumbnailSupport;

    @Activate
    public ThumbnailsWebConsole(@Reference ThumbnailSupport thumbnailSupport, @Reference Transformer transformer) {
        this.thumbnailSupport = thumbnailSupport;
        this.transformer = transformer;
    }

    @Override
    public String getTitle() {
        return CONSOLE_TITLE;
    }

    @Override
    public String getLabel() {
        return CONSOLE_LABEL;
    }

    @Override
    protected void renderContent(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {
        PrintWriter pw = httpServletResponse.getWriter();

        printSeparator(pw, "Supported Resource Types", true);
        pw.println("[Resource Type] => [MetaType Property Path]");
        thumbnailSupport.getSupportedTypes()
                .forEach(st -> pw.println(st + " => " + thumbnailSupport.getMetaTypePropertyPath(st)));

        printSeparator(pw, "Persistable Resource Types", false);
        pw.println("[Resource Type] => [Rendition Path]");
        thumbnailSupport.getPersistableTypes()
                .forEach(pt -> pw.println(pt + " => " + thumbnailSupport.getRenditionPath(pt)));

        printSeparator(pw, "Registered Thumbnail Providers", false);
        List<ThumbnailProvider> providers = ((TransformerImpl) transformer).getThumbnailProviders();
        Lists.reverse(providers).forEach(p -> pw.println(p.getClass().getName()));

        printSeparator(pw, "Registered Transformation Providers", false);
        List<TransformationHandler> handlers = ((TransformerImpl) transformer).getHandlers();
        handlers.forEach(h -> pw.println(h.getResourceType() + "=" + h.getClass().getCanonicalName()));
        pw.println("</pre>");
        pw.println("</div>");
    }

    private void printSeparator(PrintWriter pw, String title, boolean first) {
        if (!first) {
            pw.println("</pre><br/>");
        }
        pw.println("<pre>");
        pw.println(title);
        pw.println("========================");

    }

}
