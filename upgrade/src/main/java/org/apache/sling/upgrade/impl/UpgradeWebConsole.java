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
package org.apache.sling.upgrade.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.sling.upgrade.BundleEntry;
import org.apache.sling.upgrade.ConfigEntry;
import org.apache.sling.upgrade.UpgradeRequest;
import org.apache.sling.upgrade.UpgradeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web Console for the upgrading an Apache Sling instance
 */
@Component(service = Servlet.class, property = {
        "service.description=Web Console for upgrading an Apache Sling instance",
        "service.vendor=The Apache Software Foundation", "felix.webconsole.label=" + UpgradeWebConsole.APP_ROOT,
        "felix.webconsole.title=Upgrade", "felix.webconsole.category=Sling" })
public class UpgradeWebConsole extends AbstractWebConsolePlugin {

    static final String APP_ROOT = "upgrade";

    private static final Logger log = LoggerFactory.getLogger(UpgradeWebConsole.class);

    /**
     * 
     */
    private static final long serialVersionUID = 2255746886735460607L;

    @Reference
    private UpgradeService upgradeService;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#destroy()
     */
    @Override
    public void destroy() {
        // nothing needed
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (ServletFileUpload.isMultipartContent(req)) {

            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

            ServletFileUpload upload = new ServletFileUpload(factory);

            InputStream jarIs = null;
            List<FileItem> formItems;
            try {
                formItems = upload.parseRequest(req);
                if (formItems != null && !formItems.isEmpty()) {
                    for (FileItem it : formItems) {
                        if (it != null && !it.isFormField()) {
                            jarIs = it.getInputStream();
                            break;
                        }
                    }
                }
            } catch (IOException | FileUploadException e1) {
                log.error("Exception reading file input", e1);
                throw new ServletException(e1);
            }

            // start the html response, write the header, open body and main div
            PrintWriter pw = startResponse(req, resp);

            // render top navigation
            renderTopNavigation(req, pw);

            // wrap content in a separate div
            pw.println("<div id='content'>");
            renderContent(req, resp);
            writeUpgradeInfo(resp, jarIs);
            pw.println("</div>");

            // close the main div, body, and html
            endResponse(pw);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getLabel()
     */
    @Override
    public String getLabel() {
        return "upgrade";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#getServletInfo()
     */
    @Override
    public String getServletInfo() {
        return "";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
     */
    @Override
    public String getTitle() {
        return "Upgrade Service";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        // nothing needed
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Writer out = res.getWriter();

        out.write("<table class=\"content\" cellspacing=\"0\" width=\"100%\" cellpadding=\"0\">");
        out.write("<thead><tr><th class=\"content container\">Update Apache Sling</th></tr></thead>");
        out.write("<tbody><tr><td  class=\"content\">");
        out.write("<form method=\"post\" enctype=\"multipart/form-data\">");
        out.write(
                "<div><label for=\"jar\">Sling Jar</lable><br/><input type=\"file\" name=\"jar\" accepts=\"application/java-archive\"></div>");
        out.write(
                "<div><label for=\"action\">Action</lable><br/><select name=\"action\"><option>Upgrade</option><option>Preview</option></div>");
        out.write("<div><input type=\"submit\" value=\"Upload\" /></div>");
        out.write("</form></td></tr></tbody></table>");

    }

    private void writeBundle(BundleEntry be, Writer out) {
        try {
            out.write("<tr class=\"content\"><td class=\"content\">");
            if (!be.isInstalled()) {
                out.write("Install");
            } else if (be.isUpdated()) {
                out.write("Update");
            } else {
                out.write("No Action");
            }

            out.write("<td class=\"content\">" + be.getSymbolicName() + "</td>");
            out.write("<td class=\"content\">" + be.getVersion() + "</td>");
            out.write("<td class=\"content\">" + be.getStart() + "</td>");
            out.write("</tr>");
        } catch (IOException e) {
            log.error("This really shouldn't happen", e);
        }
    }

    private void writeConfig(ConfigEntry cfg, Writer out) {
        try {
            out.write("<tr class=\"content\"><td class=\"content\" colspan=\"4\">" + cfg.getPid() + "</td></tr>");
            out.write("</tr>");
        } catch (IOException e) {
            log.error("This really shouldn't happen", e);
        }
    }

    private void writeUpgradeInfo(HttpServletResponse resp, InputStream jarIs) throws IOException {
        Writer out = resp.getWriter();
        UpgradeRequest request = upgradeService.readSlingJar(jarIs);

        out.write("<table class=\"content\" cellspacing=\"0\" cellpadding=\"0\">");
        out.write("<thead><tr><th class=\"content container\" colspan=\"4\">" + request.getTitle() + " version "
                + request.getVersion());
        out.write("<br/><small>" + request.getVendor() + "</small></th></tr></thead>");

        out.write("<tbody><tr><th class=\"content container\" colspan=\"4\">Startup Bundles</h3><th></tr>");
        out.write(
                "<tr><th class=\"content\">Action</th><th class=\"content\">Bundle</th><th class=\"content\">Version</th><th class=\"content\">Start Level</th></tr>");
        request.getStartupBundles().forEach(e -> {
            writeBundle(e, out);
        });
        out.write("</tbody>");

        out.write("<tbody><tr><th class=\"content container\" colspan=\"4\">Install Bundles</h3><th></tr>");
        out.write(
                "<tr><th class=\"content\">Action</th><th class=\"content\">Bundle</th><th class=\"content\">Version</th><th class=\"content\">Start Level</th></tr>");
        request.getBundles().forEach(e -> {
            writeBundle(e, out);
        });

        out.write("</tbody>");

        out.write("<tbody><tr><th class=\"content container\" colspan=\"4\">Configurations</h3><th></tr>");
        out.write("<tr><th class=\"content\" colspan=\"4\">Configuration PID</th></tr>");
        request.getConfigs().forEach(e -> {
            writeConfig(e, out);
        });
        out.write("</tbody></table>");
    }
}
