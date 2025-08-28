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
package org.apache.sling.microsling.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.exceptions.SlingException;
import org.apache.sling.microsling.scripting.engines.freemarker.FreemarkerScriptEngine;
import org.apache.sling.microsling.scripting.engines.rhino.RhinoJavasSriptEngine;
import org.apache.sling.microsling.scripting.engines.velocity.VelocityTemplatesScriptEngine;
import org.apache.sling.microsling.scripting.helpers.ScriptFilenameBuilder;
import org.apache.sling.microsling.scripting.helpers.ScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Find scripts in the repository, based on the current Resource type.
 *  The script filename is built using the current HTTP request method name,
 *  followed by the extension of the current request and the desired script
 *  extension.
 *  For example, a "js" script for a GET request on a Resource of type some/type
 *  with request extension "html" should be stored as
 *  <pre>
 *      /sling/scripts/some/type/get.html.js
 *  </pre>
 *  in the repository.
 */
public class SlingScriptResolver {

    private static final Logger log = LoggerFactory.getLogger(SlingScriptResolver.class);

    public static final String SCRIPT_BASE_PATH = "/sling/scripts";

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    private final ScriptFilenameBuilder scriptFilenameBuilder = new ScriptFilenameBuilder();

    private Map<String, ScriptEngine> scriptEngines;

    public SlingScriptResolver() throws SlingException {
        scriptEngines = new HashMap<String, ScriptEngine>();
        addScriptEngine(new RhinoJavasSriptEngine());
        addScriptEngine(new VelocityTemplatesScriptEngine());
        addScriptEngine(new FreemarkerScriptEngine());
    }

    /**
     *
     * @param req
     * @param scriptExtension
     * @return <code>true</code> if a Script and a ScriptEngine to evaluate it
     *      could be found. Otherwise <code>false</code> is returned.
     * @throws ServletException
     * @throws IOException
     */
    public boolean evaluateScript(final HttpServletRequest req,final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final SlingRequestContext ctx = SlingRequestContext.getFromRequest(req);

            Script script = resolveScript(ctx, req.getMethod());
            if (script == null) {
                return false;
            }

            // the script helper
            ScriptHelper helper = new ScriptHelper(req, resp);

            // prepare the properties for the script
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(ScriptEngine.FILENAME, script.getScriptPath());
            props.put(ScriptEngine.SLING, helper);
            props.put(ScriptEngine.REQUEST, helper.getRequest());
            props.put(ScriptEngine.RESPONSE, helper.getResponse());
            props.put(ScriptEngine.REQUEST_CONTEXT, ctx);
            props.put(ScriptEngine.RESOURCE, ctx.getResource());
            props.put(ScriptEngine.OUT, helper.getResponse().getWriter());

            // TODO the chosen Content-Type should be available
            // based on the actual decision made...where?
            resp.setContentType(ctx.getPreferredResponseContentType() + "; charset=UTF-8");

            // evaluate the script now using the ScriptEngine
            script.getScriptEngine().eval(script.getScriptSource(), props);

            // ensure data is flushed to the underlying writer in case
            // anything has been written
            helper.getResponse().getWriter().flush();

            return true;

        } catch (IOException ioe) {
            throw ioe;
        } catch (ServletException se) {
            throw se;
        } catch (Exception e) {
            throw new SlingException("Cannot get Script: " + e.getMessage(), e);
        }
    }

    /** Try to find a script Node that can process the given request, based on the
     *  rules defined above.
     *  @return null if not found.
     */
    private Script resolveScript(final SlingRequestContext ctx, final String method) throws Exception {

        final Resource r = ctx.getResource();
        final Session s = ctx.getSession();
        Script result = null;

        if(r==null) {
            return null;
        }

        String scriptFilename = scriptFilenameBuilder.buildScriptFilename(
            method,
            ctx.getRequestPathInfo().getSelectorString(),
            ctx.getPreferredResponseContentType(),
            "*"
        );
        String scriptPath =
            SCRIPT_BASE_PATH
            + "/"
            + r.getResourceType()
            ;

        // SLING-72: if the scriptfilename contains a relative path, move that
        // to the scriptPath and make the scriptFilename a direct child pattern
        int lastSlash = scriptFilename.lastIndexOf('/');
        if (lastSlash >= 0) {
            scriptPath += "/" + scriptFilename.substring(0, lastSlash);
            scriptFilename = scriptFilename.substring(lastSlash + 1);
        }

        // this is the location of the trailing asterisk
        final int scriptExtensionOffset = scriptFilename.length() - 1;

        if(log.isDebugEnabled()) {
            log.debug("Looking for script with filename=" + scriptFilename + " under " + scriptPath);
        }

        if(s.itemExists(scriptPath)) {

            // get the item and ensure it is a node
            final Item i = s.getItem(scriptPath);
            if (i.isNode()) {
                Node parent = (Node) i;
                NodeIterator scriptNodeIterator = parent.getNodes(scriptFilename);
                while (scriptNodeIterator.hasNext()) {
                    Node scriptNode = scriptNodeIterator.nextNode();

                    // SLING-72: Require the node to be an nt:file
                    if (scriptNode.isNodeType("nt:file")) {

                        String scriptName = scriptNode.getName();
                        String scriptExt = scriptName.substring(scriptExtensionOffset);
                        ScriptEngine scriptEngine = scriptEngines.get(scriptExt);

                        if (scriptEngine != null) {
                            Script script = new Script();
                            script.setNode(scriptNode);
                            script.setScriptPath(scriptNode.getPath());
                            script.setScriptEngine(scriptEngine);
                            result = script;
                            break;
                        }
                    }
                }
            }
        }

        if(result!=null) {
            log.info("Found nt:file script node " + result.getScriptPath() + " for Resource=" + r);
        } else {
            log.debug("nt:file script node not found at path=" + scriptPath + " for Resource=" + r);
        }

        return result;
    }

    protected String filterStringForFilename(String inputString) {
        final StringBuffer sb = new StringBuffer();
        final String str = inputString.toLowerCase();
        for(int i=0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if(Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    private void addScriptEngine(ScriptEngine scriptEngine) {
        String[] extensions = scriptEngine.getExtensions();
        for (String extension : extensions) {
            scriptEngines.put(extension, scriptEngine);
        }
    }

    private static class Script {
        private Node node;
        private String scriptPath;
        private ScriptEngine scriptEngine;

        /**
         * @return the node
         */
        Node getNode() {
            return node;
        }
        /**
         * @param node the node to set
         */
        void setNode(Node node) {
            this.node = node;
        }
        /**
         * @return the scriptPath
         */
        String getScriptPath() {
            return scriptPath;
        }
        /**
         * @param scriptPath the scriptPath to set
         */
        void setScriptPath(String scriptPath) {
            this.scriptPath = scriptPath;
        }
        /**
         * @return the scriptEngine
         */
        ScriptEngine getScriptEngine() {
            return scriptEngine;
        }
        /**
         * @param scriptEngine the scriptEngine to set
         */
        void setScriptEngine(ScriptEngine scriptEngine) {
            this.scriptEngine = scriptEngine;
        }

        /**
         * @return The script stored in the node as a Reader
         */
        Reader getScriptSource() throws RepositoryException, IOException {

            // SLING-72: Cannot use primary items due to WebDAV creating
            // nt:unstructured as jcr:content node. So we just assume
            // nt:file and try to use the well-known data path
            Property property = getNode().getProperty("jcr:content/jcr:data");
            Value value = null;
            if (property.getDefinition().isMultiple()) {
                // for a multi-valued property, we take the first non-null
                // value (null values are possible in multi-valued properties)
                // TODO: verify this claim ...
                Value[] values = property.getValues();
                for (Value candidateValue : values) {
                    if (candidateValue != null) {
                        value = candidateValue;
                        break;
                    }
                }

                // incase we could not find a non-null value, we bail out
                if (value == null) {
                    throw new IOException("Cannot access " + getScriptPath());
                }
            } else {
                // for single-valued properties, we just take this value
                value = property.getValue();
            }

            // Now know how to get the input stream, we still have to decide
            // on the encoding of the stream's data. Primarily we assume it is
            // UTF-8, which is a default in many places in JCR. Secondarily
            // we try to get a jcr:encoding property besides the data property
            // to provide a possible encoding
            String encoding = "UTF-8";
            try {
                Node parent = property.getParent();
                if (parent.hasNode(JCR_ENCODING)) {
                    encoding = parent.getProperty(JCR_ENCODING).getString();
                }
            } catch (RepositoryException re) {
                // don't care if we fail for any reason here, just assume default
            }

            // access the value as a stream and return a buffered reader
            // converting the stream data using UTF-8 encoding, which is
            // the default encoding used
            InputStream input = value.getStream();
            return new BufferedReader(new InputStreamReader(input, encoding));
        }
    }
}
