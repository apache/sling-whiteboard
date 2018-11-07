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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.sling.upgrade.BundleEntry;
import org.apache.sling.upgrade.ConfigEntry;
import org.apache.sling.upgrade.RepoInitEntry;
import org.apache.sling.upgrade.StartupBundleEntry;
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

    private static final String CONTENT = "content";

    private static final String TITLE = "title";

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

        String template = getTemplate("form.html");

        out.write(template);
    }

    private static String getTemplate(String name) {
        try {
            return IOUtils.toString(UpgradeWebConsole.class.getClassLoader().getResourceAsStream(name));
        } catch (IOException e) {
            log.error("Exception loading template", e);
            return "";
        }
    }

    private static String template(String name, Map<String, String> params) {
        String template = getTemplate(name);
        StrSubstitutor sub = new StrSubstitutor(params);
        return sub.replace(template);
    }

    private static String renderBundle(BundleEntry be) {

        String action;
        if (!be.isInstalled()) {
            action = "Install";
        } else if (be.isUpdated()) {
            action = "Update";
        } else {
            action = "No Action";
        }

        return template("bundleentry.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("action", action);
                put("symbolicName", be.getSymbolicName());
                put("version", be.getVersion().toString());
                put("start", String.valueOf(be.getStart()));
            }
        });
    }

    private static String renderConfig(ConfigEntry cfg) {
        return template("configentry.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("pid", cfg.getPid());
                put("path", cfg.getPath());
                put(CONTENT, new String(cfg.getContents(), Charset.defaultCharset()).replace("\n", "<br/>"));
            }
        });
    }

    private void writeUpgradeInfo(HttpServletResponse resp, InputStream jarIs) throws IOException {
        Writer out = resp.getWriter();
        UpgradeRequest request = upgradeService.readSlingJar(jarIs);

        out.write("<table class=\"content\" cellspacing=\"0\" cellpadding=\"0\">");
        out.write(template("header.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(TITLE, request.getTitle());
                put("vendor", request.getVendor());
                put("version", request.getVersion());
            }
        }));

        out.write(template("bundles.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(CONTENT, request.getEntriesByType(StartupBundleEntry.class).stream()
                        .map(UpgradeWebConsole::renderBundle).collect(Collectors.joining()));
                put(TITLE, "Startup Bundles");
            }
        }));

        out.write(template("bundles.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(CONTENT, request.getEntriesByType(BundleEntry.class).stream().map(UpgradeWebConsole::renderBundle)
                        .collect(Collectors.joining()));
                put(TITLE, "Install Bundles");
            }
        }));

        out.write(template("config.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(CONTENT, request.getEntriesByType(ConfigEntry.class).stream().map(UpgradeWebConsole::renderConfig)
                        .collect(Collectors.joining()));
            }
        }));

        out.write(template("repoinit.html", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(CONTENT, request.getEntriesByType(RepoInitEntry.class).stream()
                        .map(UpgradeWebConsole::renderRepoInit).collect(Collectors.joining()));
            }
        }));

        out.write("</table>");
    }

    private static String renderRepoInit(RepoInitEntry ri) {

        StringBuilder sb = new StringBuilder();
        ri.getRepoInits().forEach((f, os) -> {
            StringBuilder oStr = new StringBuilder();
            os.forEach(o -> oStr.append("<li>" + o + "</li>"));
            sb.append(template("repoinitentry.html", new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("feature", f);
                    put(CONTENT, oStr.toString());
                }
            }));
        });
        return sb.toString();
    }
}
